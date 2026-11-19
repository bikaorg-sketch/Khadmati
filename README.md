# Khadmati — خدمتي 🏨

**In-room AI concierge for hotel chains**: every room carries a printed QR card.
Scanning it opens the app **directly into an AI chat** (no sign-in, no hub) that
answers FAQs (WiFi, schedules, qibla…), takes item requests, room-service orders
and cleaning scheduling, and escalates everything it cannot answer to hotel
management. Arabic-first (RTL) with full English support.

## Core flow

```
Owner generates per-room QR  →  prints PNG (Pictures/Khadmati)
Guest scans room QR          →  app installed:  opens DIRECTLY on the chat, room-aware
                                app missing:    download landing page → Play Store
Chat answers / creates requests / escalates
Owner console                →  rooms & QR, requests inbox, knowledge base, escalations
```

Deep links (on the chat destination itself):
- `https://khadmati.app/r/{roomId}?t={token}` (App Links, autoVerify pending assetlinks.json)
- `khadmati://r/{roomId}?t={token}` (dev)
- Room QR content = the https URL. The token is validated against the room's
  active credential; stale codes show a friendly blocked state with the
  front-desk phone.

### App-not-installed fallback (download landing page)

`landing/index.html` is a zero-dependency static page — host it once at
`khadmati.app` (rewrite every `/r/*` path to `index.html`) and every room QR
becomes safe to scan for guests **without** the app:

1. Installed + App Links verified → Android opens the chat directly (page never loads).
2. Installed, App Links not yet verified → the page fires an
   `intent://…#Intent;scheme=https;package=com.khadmati.app;S.browser_fallback_url=…`
   immediately, opening the app (and a `khadmati://` custom-scheme retry for
   non-Chrome browsers).
3. Not installed → after 1.4s the page shows "جاري تحويلك لتحميل التطبيق" and
   auto-redirects to the Play Store listing (`market://` on Android), with a
   manual download button always visible.

Deploy example (Firebase Hosting `firebase.json`):

```json
{
  "hosting": {
    "public": "landing",
    "rewrites": [{ "source": "/r/**", "destination": "/index.html" }]
  }
}
```

Any static host works (nginx `try_files`, Netlify redirects, Cloudflare Pages
`/_redirects`: `/r/* /index.html 200`).

## Phase 1 — done (2026-11-19)

- **Guest**: role select → guest home (scan / demo rooms) → CameraX+ML Kit QR
  scanner → **chat opens directly with room context** (hotel, room number, type).
- **Chat UI**: gradient header, chat bubbles + timestamps, animated typing
  indicator, quick-action chips (WiFi / request items / room service / cleaning /
  FAQs), bottom sheets for item picking (amenities, menu) and common questions.
- **Assistant brain** (`LocalKbAssistant`, rule-based over the knowledge base):
  distress keywords → escalate to management (with phone); maintenance report →
  MAINTENANCE request; amenities/food detection → AMENITIES / ROOM_SERVICE
  requests with items; housekeeping → HOUSEKEEPING request; menu ask → menu
  from KB; human ask → front-desk phone; KB keyword retrieval (ar/en);
  fallback → escalation. Rate limit 10 msgs/min.
- **Owner console** (demo sign-in): Rooms & QR (hotel selector, add room,
  generate/rotate per-room code, save print-ready PNG), Requests inbox
  (open → in progress → done), Knowledge editor (per hotel, ar/en), Escalations
  (view/resolve).
- **i18n**: Arabic default (`values/`), English (`values-en/`), per-app locale
  toggle (ar-SA default / en-US), RTL-safe UI.
- **Persistence**: SharedPreferences-JSON (QR tokens, KB, requests,
  escalations) — survives restarts; hotels/rooms are seeded demo data.
- **Tests**: 17 JVM unit tests (assistant intents & side effects, chat VM
  wiring) + 2 instrumented UI tests (deep link → chat directly → chip creates
  persisted request; stale token → blocked state) — all green on Pixel35 (API 35).

### Mocked (to be replaced by the cloud backend)
- Hotels/rooms are seed data (2 hotels × 4 rooms), not editable remotely.
- Owner sign-in is one tap (no accounts); QR token signing is local HMAC
  (TODO: server-side JWT + rotation history).
- No push notifications yet (requests/escalations appear in the console only).
- Chat history is per-session (requests/escalations persist, messages don't).

## Architecture

- Kotlin + Jetpack Compose (Material 3), single `:app` module, minSdk 26,
  targetSdk 35, Java 17.
- **Manual DI** (`di/ServiceLocator.kt`) — Hilt is broken on this AGP/Kotlin
  set (JavaPoet clash); all repositories are interfaces, so backends swap in
  one place only.
- Nav: `KhadmatiApp.kt` — chat route `chat/{roomId}?t={token}` carries the
  deep links (query key `t` → argument `token`); `handleDeepLink` on cold start
  and `onNewIntent` (singleTask).
- Assistant boundary: `data/AssistantRepository.kt` — phase 2 swaps in an LLM
  client implementing the same interface (API key stays server-side).

## Build & run

Prereqs (same machine layout as StayEase): JDK 17 Temurin at
`C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`, Android SDK at
`S:\Android\Sdk`, `GRADLE_USER_HOME=S:\Android\gradle-home`.

```bat
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot
set GRADLE_USER_HOME=S:\Android\gradle-home
cd /d S:\Projects\Khadmati
gradlew :app:assembleDebug
gradlew :app:testDebugUnitTest
:: UI tests — run on the Pixel35 (API 35) AVD
set ANDROID_SERIAL=emulator-5554
gradlew :app:connectedDebugAndroidTest
```

Try a deep link without a printed card:
```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -a android.intent.action.VIEW -d "https://khadmati.app/r/room_golden_302?t=<token-from-owner-qr>"
```

### Release APK (ready-to-install package)

A signed, installable build ships at **`dist/Khadmati-v1.0-release.apk`**
(~31 MB, minSdk 26 / Android 8.0+). Signing uses
`keystore/khadmati-release.jks` with credentials in
`keystore/keystore.properties` — **keep both safe and never lose them**: the
same key must sign every future update, or Android/Play will reject the
upgrade. Rebuild it:

```bat
gradlew :app:assembleRelease
copy /y app\build\outputs\apk\release\app-release.apk dist\Khadmati-v1.0-release.apk
```

Side-load via "Install unknown apps" (or `adb install`); publishing to Google
Play needs the store listing + signing arrangements from Phase 3.

## Phase roadmap

- **Phase 2 — real AI brain**: cloud LLM client behind `AssistantRepository`
  (server-side proxy holding the key), tool-calling for requests, chat history
  persistence, per-room booking context.
- **Phase 3 — cloud backend**: Firebase/REST repositories (hotels, rooms, KB,
  requests, escalations), real owner accounts, push notifications for new
  requests/escalations, App Links assetlinks.json.
- **Phase 4 — chain features**: multi-branch dashboards, staff roles
  (housekeeping/kitchen queues), guest upsell (spa, late checkout), analytics
  (FAQ gaps, response times), printable QR sheets with hotel branding.

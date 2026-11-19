package com.khadmati.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.khadmati.app.ui.KhadmatiApp
import com.khadmati.app.ui.LocalePrefs
import com.khadmati.app.ui.theme.KhadmatiTheme

class MainActivity : AppCompatActivity() {

    /** Deep links arriving while the activity is already running (QR scan). */
    private val pendingDeepLink = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // super.onCreate() MUST run before any early return, or Android throws
        // SuperNotCalledException when the locale reconciliation path triggers.
        super.onCreate(savedInstanceState)
        // Arabic-first: align the applied per-app locales with the persisted choice.
        val expected = LocalePrefs.current(this)
        val applied = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (applied != expected) {
            LocalePrefs.apply(expected) // schedules a recreation with the right locale
            return
        }
        enableEdgeToEdge()
        pendingDeepLink.value = intent
        setContent {
            KhadmatiTheme {
                KhadmatiApp(pendingDeepLink = pendingDeepLink.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingDeepLink.value = intent
    }
}

package com.khadmati.app

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.khadmati.app.di.ServiceLocator
import com.khadmati.app.domain.model.RequestType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The core promise: scanning (simulating) the room QR deep link opens the AI
 * chat directly with the room context - no hub, no sign-in - and quick actions
 * create service requests the host can see.
 */
@RunWith(AndroidJUnit4::class)
class RoomQrChatUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun launchWithDeepLink(roomId: String, token: String): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("https://khadmati.app/r/$roomId?t=$token")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun deepLinkOpensChatDirectly_andAmenityChipCreatesRequest() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Host generates the printed room code first.
        val token = ServiceLocator.qrRepository.generate("room_golden_302")

        launchWithDeepLink("room_golden_302", token)

        // Chat opens directly with the room context in the header.
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("غرفة 302", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Quick chip: request items -> sheet -> pick towels -> send.
        composeRule.onNodeWithText(context.getString(R.string.chip_amenities))
            .performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(context.getString(R.string.amenity_towels))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(context.getString(R.string.amenity_towels))[0]
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.sheet_confirm))
            .performClick()

        // The brain created a persisted AMENITIES request.
        composeRule.waitUntil(10_000) {
            ServiceLocator.serviceRequestRepository.requests.value.isNotEmpty()
        }
        val request = ServiceLocator.serviceRequestRepository.requests.value.first()
        assertEquals(RequestType.AMENITIES, request.type)
        assertEquals("room_golden_302", request.roomId)

        // Confirmation bubble appeared in the chat.
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("هيوصلك", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun staleToken_showsBlockedState() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ServiceLocator.qrRepository.generate("room_golden_401")

        // Token that was never issued (e.g. card rotated on the host side).
        launchWithDeepLink("room_golden_401", "deadbeefdeadbeefdeadbeef")

        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText(context.getString(R.string.chat_invalid_title))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}

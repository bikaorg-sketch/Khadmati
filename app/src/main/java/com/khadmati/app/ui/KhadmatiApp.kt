package com.khadmati.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.khadmati.app.ui.screens.chat.ChatScreen
import com.khadmati.app.ui.screens.host.GuestHomeScreen
import com.khadmati.app.ui.screens.host.HostHomeScreen
import com.khadmati.app.ui.screens.host.HostLoginScreen
import com.khadmati.app.ui.screens.host.RoleSelectScreen
import com.khadmati.app.ui.screens.scanner.QrScannerScreen

object Routes {
    const val ROLE_SELECT = "role_select"
    const val GUEST_HOME = "guest_home"
    const val SCANNER = "qr_scanner"
    const val HOST_LOGIN = "host_login"
    const val HOST_HOME = "host_home"
    // Query key "t" fills the "token" argument (same pattern as StayEase hub).
    const val CHAT = "chat/{roomId}?t={token}"

    fun chat(roomId: String, token: String?) = "chat/$roomId?t=${token ?: ""}"
}

/**
 * App shell. The room QR deep link opens the chat destination directly - no
 * hub, no sign-in: scanning the printed card is the guest's sign-in.
 */
@Composable
fun KhadmatiApp(pendingDeepLink: Intent?) {
    val navController = rememberNavController()

    // QR deep links arriving at MainActivity (cold start + onNewIntent).
    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let { navController.handleDeepLink(it) }
    }

    NavHost(navController = navController, startDestination = Routes.ROLE_SELECT) {
        composable(Routes.ROLE_SELECT) {
            RoleSelectScreen(
                onGuest = { navController.navigate(Routes.GUEST_HOME) },
                onHost = { navController.navigate(Routes.HOST_LOGIN) },
            )
        }
        composable(Routes.GUEST_HOME) {
            GuestHomeScreen(
                onScan = { navController.navigate(Routes.SCANNER) },
                onOpenDemoRoom = { roomId -> navController.navigate(Routes.chat(roomId, null)) },
            )
        }
        composable(Routes.SCANNER) {
            QrScannerScreen(
                onBack = { navController.popBackStack() },
                onQrDetected = { raw ->
                    val uri = Uri.parse(raw)
                    val roomId = uri.lastPathSegment ?: return@QrScannerScreen
                    navController.navigate(Routes.chat(roomId, uri.getQueryParameter("t"))) {
                        popUpTo(Routes.ROLE_SELECT)
                    }
                },
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("token") { type = NavType.StringType; defaultValue = "" },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "khadmati://r/{roomId}?t={token}" },
                navDeepLink { uriPattern = "https://khadmati.app/r/{roomId}?t={token}" },
            ),
        ) { entry ->
            val roomId = entry.arguments?.getString("roomId").orEmpty()
            val token = entry.arguments?.getString("token")?.takeIf { it.isNotBlank() }
            ChatScreen(
                roomId = roomId,
                token = token,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HOST_LOGIN) {
            HostLoginScreen(
                onSignedIn = {
                    navController.navigate(Routes.HOST_HOME) {
                        popUpTo(Routes.ROLE_SELECT)
                    }
                },
            )
        }
        composable(Routes.HOST_HOME) {
            HostHomeScreen()
        }
    }
}

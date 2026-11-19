package com.khadmati.app.ui.screens.host

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khadmati.app.R
import com.khadmati.app.di.ServiceLocator
import com.khadmati.app.ui.LocalePrefs
import com.khadmati.app.ui.theme.isArabicLocale

/**
 * Entry point: guest (scan the room QR) or hotel owner (management console).
 * Arabic-first with a language toggle.
 */
@Composable
fun RoleSelectScreen(
    onGuest: () -> Unit,
    onHost: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            TextButton(
                onClick = {
                    val next = LocalePrefs.other(LocalePrefs.current(context))
                    LocalePrefs.save(context, next)
                    LocalePrefs.apply(next)
                },
            ) {
                Text(stringResource(R.string.lang_toggle))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isArabicLocale()) "خ" else "K",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                text = stringResource(R.string.role_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.role_slogan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )
            RoleCard(
                title = stringResource(R.string.role_guest),
                note = stringResource(R.string.role_guest_note),
                onClick = onGuest,
            )
            RoleCard(
                title = stringResource(R.string.role_host),
                note = stringResource(R.string.role_host_note),
                onClick = onHost,
            )
        }
    }
}

@Composable
private fun RoleCard(title: String, note: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Guest landing: scan the room QR, or tap a demo room without scanning. */
@Composable
fun GuestHomeScreen(
    onScan: () -> Unit,
    onOpenDemoRoom: (roomId: String) -> Unit,
) {
    val isArabic = isArabicLocale()
    val hotels = remember { ServiceLocator.hotelRepository.hotels.value }
    val rooms = remember { ServiceLocator.hotelRepository.allRooms() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.guest_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.guest_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        Button(
            onClick = onScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.guest_scan), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = stringResource(R.string.guest_demo_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(R.string.guest_demo_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        hotels.forEach { hotel ->
            Text(
                text = if (isArabic) hotel.nameAr else hotel.nameEn,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            rooms.filter { it.hotelId == hotel.id }.forEach { room ->
                OutlinedButton(
                    onClick = { onOpenDemoRoom(room.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (isArabic) {
                            "غرفة ${room.number} - ${room.typeAr} (طابق ${room.floor})"
                        } else {
                            "Room ${room.number} - ${room.typeEn} (floor ${room.floor})"
                        },
                    )
                }
            }
        }
    }
}

/** Demo owner sign-in (real accounts come with the cloud backend). */
@Composable
fun HostLoginScreen(
    onSignedIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.host_login_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.host_login_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(
            onClick = onSignedIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.host_login_button))
        }
    }
}

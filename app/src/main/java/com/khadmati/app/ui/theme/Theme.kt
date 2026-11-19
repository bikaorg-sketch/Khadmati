package com.khadmati.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm hospitality palette: gold primary, cream surfaces, green accent.
private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5800),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF6D5C28),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7E1AC),
    onSecondaryContainer = Color(0xFF231A00),
    tertiary = Color(0xFF41673C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC2EDA9),
    onTertiaryContainer = Color(0xFF0A2005),
    background = Color(0xFFFFF8EF),
    onBackground = Color(0xFF201B11),
    surface = Color(0xFFFFF8EF),
    onSurface = Color(0xFF201B11),
    surfaceVariant = Color(0xFFEEE1CF),
    onSurfaceVariant = Color(0xFF4C4639),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEDC14C),
    onPrimary = Color(0xFF3D2E00),
    primaryContainer = Color(0xFF5B4300),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = Color(0xFFDCC58F),
    onSecondary = Color(0xFF382F00),
    secondaryContainer = Color(0xFF52451A),
    onSecondaryContainer = Color(0xFFF7E1AC),
    tertiary = Color(0xFFA8D18E),
    onTertiary = Color(0xFF102908),
    tertiaryContainer = Color(0xFF2B4E23),
    onTertiaryContainer = Color(0xFFC2EDA9),
    background = Color(0xFF191307),
    onBackground = Color(0xFFEDE1D4),
    surface = Color(0xFF191307),
    onSurface = Color(0xFFEDE1D4),
    surfaceVariant = Color(0xFF4C4639),
    onSurfaceVariant = Color(0xFFD0C4B1),
)

@Composable
fun KhadmatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

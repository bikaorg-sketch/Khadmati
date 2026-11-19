package com.khadmati.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** True when the active app locale is Arabic (drives data-language picks). */
@Composable
fun isArabicLocale(): Boolean =
    LocalConfiguration.current.locales[0].language == "ar"

/** Convenience for reading colors from the design system. */
@Composable
fun khadmatiColors() = MaterialTheme.colorScheme

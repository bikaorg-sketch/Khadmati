package com.khadmati.app.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Persists the in-app language and applies it via per-app locales.
 * Arabic is the default (primary market: KSA & Gulf), English is the secondary.
 */
object LocalePrefs {
    private const val PREFS = "khadmati_prefs"
    private const val KEY_LOCALE = "app_locale"
    const val ARABIC = "ar-SA"
    const val ENGLISH = "en-US"
    private const val DEFAULT = ARABIC

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE, DEFAULT) ?: DEFAULT

    fun save(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOCALE, tag).apply()
    }

    /** Applies the locale app-wide; triggers activity recreation. */
    fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** The language tag offered by the toggle button. */
    fun other(tag: String): String = if (tag == ARABIC) ENGLISH else ARABIC
}

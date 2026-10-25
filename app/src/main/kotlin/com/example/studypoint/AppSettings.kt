package com.example.studypoint

import android.content.Context
import android.content.res.Configuration

/**
 * App-wide display settings, persisted in SharedPreferences (same file the
 * language setting uses). Theme applies to the Compose screens (Home and
 * Profile); font size applies to AI Tutor chat text, where reading text is
 * reflowable. PDF pages themselves are fixed-layout and cannot be resized.
 */
object AppSettings {

    private const val PREFS = "app_settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_FONT_SIZE = "font_size"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    const val FONT_SMALL = "small"
    const val FONT_MEDIUM = "medium"
    const val FONT_LARGE = "large"

    fun getThemeMode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, mode).apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        return when (getThemeMode(context)) {
            THEME_DARK -> true
            THEME_LIGHT -> false
            else -> {
                val nightMask = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun getFontSize(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FONT_SIZE, FONT_MEDIUM) ?: FONT_MEDIUM
    }

    fun setFontSize(context: Context, size: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_FONT_SIZE, size).apply()
    }

    /** AI chat answer text size in sp. */
    fun chatTextSizeSp(context: Context): Int {
        return when (getFontSize(context)) {
            FONT_SMALL -> 13
            FONT_LARGE -> 17
            else -> 15
        }
    }

    /** AI chat answer line height in sp. */
    fun chatLineHeightSp(context: Context): Int {
        return when (getFontSize(context)) {
            FONT_SMALL -> 18
            FONT_LARGE -> 24
            else -> 21
        }
    }
}

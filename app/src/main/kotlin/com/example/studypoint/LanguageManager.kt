package com.example.studypoint

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Manages app language switching between English and Urdu.
 * Persists choice in SharedPreferences.
 */
object LanguageManager {

    private const val PREFS = "app_settings"
    private const val KEY_LANG = "app_language"

    const val LANG_ENGLISH = "en"
    const val LANG_URDU = "ur"

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, LANG_ENGLISH) ?: LANG_ENGLISH
    }

    fun setLanguage(context: Context, langCode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, langCode).apply()
        applyLanguage(langCode)
    }

    fun applyLanguage(langCode: String) {
        val locales = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun applySavedLanguage(context: Context) {
        applyLanguage(getSavedLanguage(context))
    }

    fun isUrdu(context: Context): Boolean = getSavedLanguage(context) == LANG_URDU

    fun toggle(context: Context): String {
        val next = if (isUrdu(context)) LANG_ENGLISH else LANG_URDU
        setLanguage(context, next)
        return next
    }
}

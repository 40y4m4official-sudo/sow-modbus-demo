package com.example.meterdemo.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class AppLanguageManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCurrentLanguage(): AppLanguage {
        return AppLanguage.fromTag(preferences.getString(KEY_LANGUAGE_TAG, AppLanguage.ENGLISH.tag))
    }

    fun applyStoredLanguage() {
        applyLanguage(getCurrentLanguage())
    }

    fun setLanguage(language: AppLanguage) {
        preferences.edit()
            .putString(KEY_LANGUAGE_TAG, language.tag)
            .apply()
        applyLanguage(language)
    }

    private fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.tag)
        )
    }

    private companion object {
        private const val PREFS_NAME = "app_language_prefs"
        private const val KEY_LANGUAGE_TAG = "language_tag"
    }
}

package com.example.meterdemo.localization

import androidx.annotation.StringRes
import com.example.meterdemo.R

enum class AppLanguage(
    val tag: String,
    @StringRes val labelRes: Int
) {
    ENGLISH("en", R.string.language_english),
    JAPANESE("ja", R.string.language_japanese);

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag == tag } ?: ENGLISH
        }
    }
}

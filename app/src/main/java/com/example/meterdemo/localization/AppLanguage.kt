package com.example.meterdemo.localization

enum class AppLanguage(
    val tag: String,
    val fixedLabel: String
) {
    ENGLISH("en", "English"),
    JAPANESE("ja", "“ú–{Œê");

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag == tag } ?: ENGLISH
        }
    }
}

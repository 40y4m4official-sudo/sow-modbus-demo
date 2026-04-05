package com.example.meterdemo.localization

enum class AppLanguage(
    val tag: String,
    val fixedLabel: String
) {
    ENGLISH("en", "English"),
    JAPANESE("ja", "\u65E5\u672C\u8A9E");

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag == tag } ?: ENGLISH
        }
    }
}
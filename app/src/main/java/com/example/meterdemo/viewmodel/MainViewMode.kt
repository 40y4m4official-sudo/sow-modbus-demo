package com.example.meterdemo.viewmodel

enum class MainViewMode(val label: String) {
    CARD("Card View"),
    LIST("List View");

    fun next(): MainViewMode {
        return when (this) {
            CARD -> LIST
            LIST -> CARD
        }
    }

    companion object {
        fun fromStoredName(name: String?): MainViewMode {
            return entries.firstOrNull { it.name == name } ?: CARD
        }
    }
}

package com.example.meterdemo.viewmodel

enum class AppMode {
    METER_DEMO,
    COMM_ANALYSIS;

    companion object {
        fun fromStoredName(name: String?): AppMode {
            return entries.firstOrNull { it.name == name } ?: METER_DEMO
        }
    }
}

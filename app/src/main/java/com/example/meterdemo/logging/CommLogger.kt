package com.example.meterdemo.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CommLogger(
    private val maxEntries: Int = 200
) {
    private val _logs = MutableStateFlow<List<CommLog>>(emptyList())
    val logs: StateFlow<List<CommLog>> = _logs.asStateFlow()

    fun add(log: CommLog) {
        val updated = listOf(log) + _logs.value
        _logs.value = updated.take(maxEntries)
    }

    fun rx(hex: String, note: String = "") {
        add(CommLog(direction = Direction.RX, hex = hex, note = note))
    }

    fun tx(hex: String, note: String = "") {
        add(CommLog(direction = Direction.TX, hex = hex, note = note))
    }

    fun info(message: String) {
        add(CommLog(direction = Direction.INFO, hex = "", note = message))
    }

    fun error(message: String) {
        add(CommLog(direction = Direction.ERROR, hex = "", note = message))
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
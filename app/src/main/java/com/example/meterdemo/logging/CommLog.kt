package com.example.meterdemo.logging

data class CommLog(
    val category: CommCategory,
    val direction: Direction,
    val hex: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class CommCategory {
    SYSTEM,
    MODBUS,
    USB
}

enum class Direction {
    RX,
    TX,
    INFO,
    ERROR
}

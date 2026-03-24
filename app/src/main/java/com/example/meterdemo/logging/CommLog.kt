package com.example.meterdemo.logging

data class CommLog(
    val direction: Direction,
    val hex: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class Direction {
    RX,
    TX,
    INFO,
    ERROR
}
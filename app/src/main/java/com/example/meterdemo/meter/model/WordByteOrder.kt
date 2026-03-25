package com.example.meterdemo.meter.model

enum class WordByteOrder(
    val label: String
) {
    MSB_MSB("MSB+MSB"),
    LSB_LSB("LSB+LSB"),
    MSB_LSB("MSB+LSB"),
    LSB_MSB("LSB+MSB");

    fun next(): WordByteOrder {
        val values = entries.toTypedArray()
        return values[(ordinal + 1) % values.size]
    }
}

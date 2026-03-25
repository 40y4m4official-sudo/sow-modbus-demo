package com.example.meterdemo.meter.model

enum class DataType {
    INT16,
    UINT16,
    INT32,
    UINT32,
    FLOAT32;

    val registerCount: Int
        get() = when (this) {
            INT16, UINT16 -> 1
            INT32, UINT32, FLOAT32 -> 2
        }

    fun next(): DataType {
        val values = entries.toTypedArray()
        return values[(ordinal + 1) % values.size]
    }
}

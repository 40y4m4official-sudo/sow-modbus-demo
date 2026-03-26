package com.example.meterdemo.meter.model

enum class DataType {
    INT,
    FLOAT;

    fun next(): DataType {
        val values = entries.toTypedArray()
        return values[(ordinal + 1) % values.size]
    }

    companion object {
        fun fromStoredName(name: String, registerCount: Int): DataType {
            return when (name) {
                INT.name, "INT16", "UINT16", "INT32", "UINT32" -> INT
                FLOAT.name, "FLOAT32" -> FLOAT
                else -> if (registerCount == 2) FLOAT else INT
            }
        }
    }
}

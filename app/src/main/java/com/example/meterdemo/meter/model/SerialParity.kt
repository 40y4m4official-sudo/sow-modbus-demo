package com.example.meterdemo.meter.model

enum class SerialParity(
    val label: String,
    val profileValue: Int
) {
    NONE("None", 0),
    ODD("Odd", 1),
    EVEN("Even", 2);

    fun next(): SerialParity {
        val values = entries.toTypedArray()
        return values[(ordinal + 1) % values.size]
    }

    companion object {
        fun fromProfileValue(value: Int): SerialParity {
            return entries.firstOrNull { it.profileValue == value } ?: EVEN
        }
    }
}

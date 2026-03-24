package com.example.meterdemo.meter.model

data class MeterPoint(
    val name: String,
    val address: Int,
    val registerCount: Int,
    val gain: Int,
    val dataType: DataType,
    val unit: String = "",
    val initialRawValue: Int
) {
    fun formattedValue(rawValue: Int): String {
        return if (gain <= 1) {
            if (unit.isBlank()) {
                rawValue.toString()
            } else {
                "$rawValue $unit"
            }
        } else {
            val scaled = rawValue.toDouble() / gain.toDouble()
            if (unit.isBlank()) {
                scaled.toString()
            } else {
                "$scaled $unit"
            }
        }
    }
}
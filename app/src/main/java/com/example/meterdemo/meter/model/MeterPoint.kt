package com.example.meterdemo.meter.model

data class MeterPoint(
    val name: String,
    val address: Int,
    val registerCount: Int,
    val gain: Double,
    val dataType: DataType,
    val unit: String = "",
    val initialRawValue: Int,
    val wordByteOrder: WordByteOrder = WordByteOrder.MSB_MSB
) {
    fun formattedValue(rawValue: Int): String {
        return if (gain <= 1.0) {
            if (unit.isBlank()) {
                rawValue.toString()
            } else {
                "$rawValue $unit"
            }
        } else {
            val scaled = rawValue.toDouble() / gain
            if (unit.isBlank()) {
                scaled.toString()
            } else {
                "$scaled $unit"
            }
        }
    }
}

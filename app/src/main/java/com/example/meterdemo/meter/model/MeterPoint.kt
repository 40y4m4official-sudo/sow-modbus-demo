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
    fun decodedValue(rawValue: Int): Double {
        return when (dataType) {
            DataType.INT16 -> rawValue.toShort().toDouble()
            DataType.UINT16 -> (rawValue and 0xFFFF).toDouble()
            DataType.INT32 -> rawValue.toDouble()
            DataType.UINT32 -> (rawValue.toLong() and 0xFFFFFFFFL).toDouble()
            DataType.FLOAT32 -> Float.fromBits(rawValue).toDouble()
        }
    }

    fun rawInputValue(rawValue: Int): String {
        return when (dataType) {
            DataType.FLOAT32 -> Float.fromBits(rawValue).toString()
            DataType.UINT32 -> (rawValue.toLong() and 0xFFFFFFFFL).toString()
            DataType.UINT16 -> (rawValue and 0xFFFF).toString()
            else -> rawValue.toString()
        }
    }

    fun formattedValue(rawValue: Int): String {
        val decoded = decodedValue(rawValue)
        val displayValue = if (gain <= 1.0) decoded else decoded / gain
        val text = if (displayValue == displayValue.toLong().toDouble()) {
            displayValue.toLong().toString()
        } else {
            displayValue.toString()
        }

        return if (unit.isBlank()) {
            text
        } else {
            "$text $unit"
        }
    }
}

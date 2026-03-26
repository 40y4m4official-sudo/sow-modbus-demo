package com.example.meterdemo.meter.model

import java.math.BigDecimal
import java.math.RoundingMode

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
    private val displayScale: Int?
        get() = when {
            unit == "V" -> 2
            unit == "A" -> 1
            unit == "kW" || unit == "kVar" || unit == "kVA" -> 3
            isPowerFactorPoint() -> 3
            else -> null
        }

    fun decodedValue(rawValue: Int): Double {
        return when (dataType) {
            DataType.FLOAT -> Float.fromBits(rawValue).toDouble()
            DataType.INT -> if (registerCount == 1) rawValue.toShort().toDouble() else rawValue.toDouble()
        }
    }

    fun rawInputValue(rawValue: Int): String {
        return when (dataType) {
            DataType.FLOAT -> Float.fromBits(rawValue).toString()
            DataType.INT -> if (registerCount == 1) rawValue.toShort().toString() else rawValue.toString()
        }
    }

    fun displayValue(rawValue: Int): Double {
        val decoded = decodedValue(rawValue)
        return if (gain <= 1.0) decoded else decoded / gain
    }

    fun displayInputValue(rawValue: Int): String {
        val value = displayValue(rawValue)
        return formatDisplayNumber(value, displayScale)
    }

    fun formattedValue(rawValue: Int): String {
        val value = displayValue(rawValue)
        val text = formatDisplayNumber(value, displayScale)

        return if (unit.isBlank()) {
            text
        } else {
            "$text $unit"
        }
    }

    private fun isPowerFactorPoint(): Boolean {
        return name.contains("力率") || name.contains("Power Factor", ignoreCase = true)
    }

    companion object {
        fun formatDisplayNumber(value: Double, scale: Int? = null): String {
            if (value == value.toLong().toDouble()) {
                return value.toLong().toString()
            }

            val resolvedScale = scale ?: 6

            return BigDecimal.valueOf(value)
                .setScale(resolvedScale, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }
    }
}

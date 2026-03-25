package com.example.meterdemo.meter.repository

import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

class MeterRepository(
    initialProfile: MeterProfile
) {
    private var currentProfile: MeterProfile = initialProfile
    private var currentSlaveId: Int = initialProfile.slaveId

    private val rawValues: MutableMap<Int, Int> = linkedMapOf()

    init {
        loadProfile(initialProfile)
    }

    fun loadProfile(profile: MeterProfile) {
        currentProfile = profile
        currentSlaveId = profile.slaveId
        rawValues.clear()
        profile.points.forEach { point ->
            rawValues[point.address] = point.initialRawValue
        }
    }

    fun getProfile(): MeterProfile = currentProfile

    fun getSlaveId(): Int = currentSlaveId

    fun setSlaveId(slaveId: Int): Boolean {
        if (slaveId !in 1..247) return false
        currentSlaveId = slaveId
        return true
    }

    fun getFunctionCode(): Int = currentProfile.functionCode

    fun getAllPoints(): List<MeterPoint> = currentProfile.points

    fun findPoint(address: Int): MeterPoint? = currentProfile.findPoint(address)

    fun hasAddress(address: Int): Boolean = rawValues.containsKey(address)

    fun getRawValue(address: Int): Int? = rawValues[address]

    fun requireRawValue(address: Int): Int {
        return rawValues[address]
            ?: throw IllegalArgumentException("Address not found: $address")
    }

    fun setRawValue(address: Int, value: Int): Boolean {
        if (!rawValues.containsKey(address)) return false
        rawValues[address] = value
        return true
    }

    fun resetCurrentProfileValues() {
        currentProfile.points.forEach { point ->
            rawValues[point.address] = point.initialRawValue
        }
    }

    fun getFormattedValue(address: Int): String? {
        val point = findPoint(address) ?: return null
        val raw = getRawValue(address) ?: return null
        return point.formattedValue(raw)
    }

    fun snapshot(): List<MeterValueSnapshot> {
        return currentProfile.points.map { point ->
            val raw = rawValues[point.address] ?: point.initialRawValue
            MeterValueSnapshot(
                name = point.name,
                address = point.address,
                registerCount = point.registerCount,
                gain = point.gain,
                unit = point.unit,
                rawValue = raw,
                formattedValue = point.formattedValue(raw)
            )
        }
    }
}

data class MeterValueSnapshot(
    val name: String,
    val address: Int,
    val registerCount: Int,
    val gain: Int,
    val unit: String,
    val rawValue: Int,
    val formattedValue: String
)

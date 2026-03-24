package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.MeterProfile

object MeterProfiles {
    private val profiles: List<MeterProfile> = listOf(
        SampleMeterV1Profile.profile
    )

    fun all(): List<MeterProfile> = profiles

    fun default(): MeterProfile = SampleMeterV1Profile.profile

    fun findByModelId(modelId: String): MeterProfile? {
        return profiles.firstOrNull { it.modelId == modelId }
    }
}
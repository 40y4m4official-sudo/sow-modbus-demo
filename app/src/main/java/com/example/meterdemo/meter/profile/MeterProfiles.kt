package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.MeterProfile

object MeterProfiles {
    private val profiles: List<MeterProfile> = listOf(
        BackUpCtProfile.profile,
        MitsubishiMe110SsrMbProfile.profile,
        Dtsu666HwProfile.profile,
        YadaYds60_80Profile.profile,
        WaveEnergyPwm72Profile.profile,
        Drpr72Dvrr72Profile.profile
    )

    fun all(): List<MeterProfile> = profiles

    fun default(): MeterProfile = BackUpCtProfile.profile

    fun findByModelId(modelId: String): MeterProfile? {
        return profiles.firstOrNull { it.modelId == modelId }
    }
}

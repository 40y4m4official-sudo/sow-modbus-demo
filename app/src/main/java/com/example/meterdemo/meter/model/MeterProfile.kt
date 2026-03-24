package com.example.meterdemo.meter.model

data class MeterProfile(
    val modelId: String,
    val displayName: String,
    val slaveId: Int,
    val baudRate: Int,
    val dataBits: Int,
    val parity: Int,
    val stopBits: Int,
    val functionCode: Int,
    val points: List<MeterPoint>
) {
    val pointByAddress: Map<Int, MeterPoint> = points.associateBy { it.address }

    fun findPoint(address: Int): MeterPoint? = pointByAddress[address]
}
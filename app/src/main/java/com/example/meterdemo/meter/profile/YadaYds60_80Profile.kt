package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.model.SignalType

object YadaYds60_80Profile {
    val profile = MeterProfile(
        modelId = "yada-yds60-80",
        displayName = "YADA-YDS60-80",
        slaveId = 2,
        baudRate = 9600,
        dataBits = 8,
        parity = 0,
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            floatPoint("A相電圧", 2110, 1.0, SignalType.PHASE_A_VOLTAGE, 6600.0),
            floatPoint("B相電圧", 2112, 1.0, SignalType.PHASE_B_VOLTAGE, 6600.0),
            floatPoint("C相電圧", 2114, 1.0, SignalType.PHASE_C_VOLTAGE, 6600.0),
            floatPoint("A-B線電圧", 2118, 1.0, SignalType.LINE_VOLTAGE_AB, 6600.0),
            floatPoint("B-C線電圧", 2120, 1.0, SignalType.LINE_VOLTAGE_BC, 6600.0),
            floatPoint("C-A線電圧", 2122, 1.0, SignalType.LINE_VOLTAGE_CA, 6600.0),
            floatPoint("A相電流", 2102, 1.0, SignalType.PHASE_A_CURRENT, 4.0),
            floatPoint("B相電流", 2104, 1.0, SignalType.PHASE_B_CURRENT, 5.0),
            floatPoint("C相電流", 2106, 1.0, SignalType.PHASE_C_CURRENT, 6.0),
            floatPoint("有効電力", 2264, 1000.0, SignalType.ACTIVE_POWER_TOTAL, 94.0),
            floatPoint("A相有効電力", 2266, 1000.0, SignalType.PHASE_A_ACTIVE_POWER, 25.0),
            floatPoint("B相有効電力", 2268, 1000.0, SignalType.PHASE_B_ACTIVE_POWER, 31.0),
            floatPoint("C相有効電力", 2270, 1000.0, SignalType.PHASE_C_ACTIVE_POWER, 38.0),
            floatPoint("無効電力", 2272, 1000.0, SignalType.REACTIVE_POWER_TOTAL, 31.0),
            floatPoint("皮相電力", 2142, 1000.0, SignalType.APPARENT_POWER_TOTAL, 99.0),
            floatPoint("合計有効電力量", 2158, 1.0, SignalType.TOTAL_ACTIVE_ENERGY, 1236.0),
            floatPoint("合計無効電力量", 2206, 1.0, SignalType.TOTAL_REACTIVE_ENERGY, 0.0),
            floatPoint("正方向合計有効電力量", 2166, 1.0, SignalType.FORWARD_ACTIVE_ENERGY_TOTAL, 1235.0),
            floatPoint("正方向合計無効電力量", 2214, 1.0, SignalType.FORWARD_REACTIVE_ENERGY_TOTAL, 0.0),
            floatPoint("負方向合計有効電力量", 2174, 1.0, SignalType.REVERSE_ACTIVE_ENERGY_TOTAL, 1.0),
            floatPoint("負方向合計無効電力量", 2222, 1.0, SignalType.REVERSE_REACTIVE_ENERGY_TOTAL, 0.0)
        )
    )

    private fun floatPoint(
        name: String,
        address: Int,
        gain: Double,
        signalType: SignalType,
        displayValue: Double
    ): MeterPoint {
        val rawValue = if (gain <= 1.0) {
            displayValue.toFloat()
        } else {
            (displayValue * gain).toFloat()
        }

        return MeterPoint(
            name = name,
            address = address,
            registerCount = 2,
            gain = gain,
            dataType = DataType.FLOAT,
            unit = signalType.defaultUnit,
            initialRawValue = rawValue.toRawBits(),
            signalType = signalType
        )
    }
}

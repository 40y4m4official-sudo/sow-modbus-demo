package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.model.SignalType

object MitsubishiMe110SsrMbProfile {
    val profile = MeterProfile(
        modelId = "mitsubishi-me110ssr-mb",
        displayName = "Mitsubishi-ME110SSR-MB",
        slaveId = 2,
        baudRate = 19200,
        dataBits = 8,
        parity = 2,
        stopBits = 1,
        functionCode = 0x03,
        points = buildList {
            addAll(dummyBlock(512, 520))

            add(point("A相電流", 768, "A", 1, SignalType.PHASE_A_CURRENT))
            add(point("B相電流", 769, "A", 2, SignalType.PHASE_B_CURRENT))
            add(point("C相電流", 770, "A", 3, SignalType.PHASE_C_CURRENT))
            addAll(dummyBlock(771, 777))
            add(point("A-B線電圧", 778, "V", 6600, SignalType.LINE_VOLTAGE_AB))
            add(point("B-C線電圧", 779, "V", 6600, SignalType.LINE_VOLTAGE_BC))
            add(point("C-A線電圧", 780, "V", 6600, SignalType.LINE_VOLTAGE_CA))
            add(point("ダミー781", 781, "", 0))
            add(point("A相電圧", 782, "V", 6600, SignalType.PHASE_A_VOLTAGE))
            add(point("B相電圧", 783, "V", 6600, SignalType.PHASE_B_VOLTAGE))
            add(point("C相電圧", 784, "V", 6600, SignalType.PHASE_C_VOLTAGE))
            addAll(dummyBlock(785, 790))
            add(point("A相有効電力", 791, "kW", 6, SignalType.PHASE_A_ACTIVE_POWER))
            add(point("B相有効電力", 792, "kW", 13, SignalType.PHASE_B_ACTIVE_POWER))
            add(point("C相有効電力", 793, "kW", 19, SignalType.PHASE_C_ACTIVE_POWER))
            add(point("有効電力", 794, "kW", 38, SignalType.ACTIVE_POWER_TOTAL))
            addAll(dummyBlock(795, 801))
            add(point("無効電力", 802, "kVar", 12, SignalType.REACTIVE_POWER_TOTAL))
            addAll(dummyBlock(803, 805))
            add(point("皮相電力", 806, "kVA", 4, SignalType.APPARENT_POWER_TOTAL))

            add(point32("正方向合計有効電力量", 1304, "kWh", 1235, SignalType.FORWARD_ACTIVE_ENERGY_TOTAL))
            add(point32("負方向合計有効電力量", 1306, "kWh", 1, SignalType.REVERSE_ACTIVE_ENERGY_TOTAL))
        }
    )

    private fun dummyBlock(start: Int, end: Int): List<MeterPoint> {
        return (start..end).map { address ->
            point("ダミー$address", address, "", 0)
        }
    }

    private fun point(
        name: String,
        address: Int,
        unit: String,
        initialRawValue: Int,
        signalType: SignalType = SignalType.CUSTOM
    ): MeterPoint {
        return MeterPoint(
            name = name,
            address = address,
            registerCount = 1,
            gain = 1.0,
            dataType = DataType.INT,
            unit = unit,
            initialRawValue = initialRawValue,
            signalType = signalType
        )
    }

    private fun point32(
        name: String,
        address: Int,
        unit: String,
        initialRawValue: Int,
        signalType: SignalType
    ): MeterPoint {
        return MeterPoint(
            name = name,
            address = address,
            registerCount = 2,
            gain = 1.0,
            dataType = DataType.INT,
            unit = unit,
            initialRawValue = initialRawValue,
            signalType = signalType
        )
    }
}

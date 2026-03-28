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
        points = listOf(
            MeterPoint("A相電圧", 782, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.PHASE_A_VOLTAGE),
            MeterPoint("B相電圧", 783, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.PHASE_B_VOLTAGE),
            MeterPoint("C相電圧", 784, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.PHASE_C_VOLTAGE),
            MeterPoint("A-B線電圧", 778, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.LINE_VOLTAGE_AB),
            MeterPoint("B-C線電圧", 779, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.LINE_VOLTAGE_BC),
            MeterPoint("C-A線電圧", 780, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.LINE_VOLTAGE_CA),
            MeterPoint("A相電流", 768, 1, 1.0, DataType.INT, "A", 1, signalType = SignalType.PHASE_A_CURRENT),
            MeterPoint("B相電流", 769, 1, 1.0, DataType.INT, "A", 2, signalType = SignalType.PHASE_B_CURRENT),
            MeterPoint("C相電流", 770, 1, 1.0, DataType.INT, "A", 3, signalType = SignalType.PHASE_C_CURRENT),
            MeterPoint("有効電力", 794, 1, 10000.0, DataType.INT, "kW", 376200, signalType = SignalType.ACTIVE_POWER_TOTAL),
            MeterPoint("A相有効電力", 791, 1, 10000.0, DataType.INT, "kW", 62700, signalType = SignalType.PHASE_A_ACTIVE_POWER),
            MeterPoint("B相有効電力", 792, 1, 10000.0, DataType.INT, "kW", 125400, signalType = SignalType.PHASE_B_ACTIVE_POWER),
            MeterPoint("C相有効電力", 793, 1, 10000.0, DataType.INT, "kW", 188100, signalType = SignalType.PHASE_C_ACTIVE_POWER),
            MeterPoint("無効電力", 802, 1, 10000.0, DataType.INT, "kVar", 123500, signalType = SignalType.REACTIVE_POWER_TOTAL),
            MeterPoint("皮相電力", 806, 1, 10000.0, DataType.INT, "kVA", 39600, signalType = SignalType.APPARENT_POWER_TOTAL),
            MeterPoint("正方向合計有効電力量", 1304, 2, 1.0, DataType.INT, "kWh", 1235, signalType = SignalType.FORWARD_ACTIVE_ENERGY_TOTAL),
            MeterPoint("負方向合計有効電力量", 1306, 2, 1.0, DataType.INT, "kWh", 1, signalType = SignalType.REVERSE_ACTIVE_ENERGY_TOTAL)
        )
    )
}

package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.model.SignalType

object BackUpCtProfile {
    val profile = MeterProfile(
        modelId = "backup-ct",
        displayName = "BackUp-CT",
        slaveId = 2,
        baudRate = 9600,
        dataBits = 8,
        parity = 0,
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            MeterPoint("A相電圧", 37101, 2, 10.0, DataType.INT, "V", 1010, signalType = SignalType.PHASE_A_VOLTAGE),
            MeterPoint("B相電圧", 37103, 2, 10.0, DataType.INT, "V", 1010, signalType = SignalType.PHASE_B_VOLTAGE),
            MeterPoint("A相電流", 37107, 2, 100.0, DataType.INT, "A", 500, signalType = SignalType.PHASE_A_CURRENT),
            MeterPoint("B相電流", 37109, 2, 100.0, DataType.INT, "A", 400, signalType = SignalType.PHASE_B_CURRENT),
            MeterPoint("有効電力", 37113, 2, 1000.0, DataType.INT, "kW", 863550, signalType = SignalType.ACTIVE_POWER_TOTAL),
            MeterPoint("無効電力", 37115, 2, 1000.0, DataType.INT, "kVar", 283600, signalType = SignalType.REACTIVE_POWER_TOTAL),
            MeterPoint("力率", 37117, 2, 1000.0, DataType.INT, "", 950, signalType = SignalType.POWER_FACTOR),
            MeterPoint("正方向合計有効電力量", 37119, 2, 100.0, DataType.INT, "kWh", 12345, signalType = SignalType.FORWARD_ACTIVE_ENERGY_TOTAL),
            MeterPoint("負方向合計有効電力量", 37121, 2, 100.0, DataType.INT, "kWh", 4321, signalType = SignalType.REVERSE_ACTIVE_ENERGY_TOTAL),
            MeterPoint("合計無効電力量", 37123, 2, 100.0, DataType.INT, "kVarh", 111100, signalType = SignalType.TOTAL_REACTIVE_ENERGY),
            MeterPoint("A-B線電圧", 37126, 2, 10.0, DataType.INT, "V", 2020, signalType = SignalType.LINE_VOLTAGE_AB),
            MeterPoint("B-C線電圧", 37128, 2, 10.0, DataType.INT, "V", 1010, signalType = SignalType.LINE_VOLTAGE_BC),
            MeterPoint("C-A線電圧", 37130, 2, 10.0, DataType.INT, "V", 1010, signalType = SignalType.LINE_VOLTAGE_CA),
            MeterPoint("A相有効電力", 37132, 2, 1000.0, DataType.INT, "kW", 479750, signalType = SignalType.PHASE_A_ACTIVE_POWER),
            MeterPoint("B相有効電力", 37134, 2, 1000.0, DataType.INT, "kW", 383800, signalType = SignalType.PHASE_B_ACTIVE_POWER)
        )
    )
}

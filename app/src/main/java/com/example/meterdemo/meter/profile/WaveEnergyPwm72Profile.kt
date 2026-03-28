package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.model.SignalType

object WaveEnergyPwm72Profile {
    val profile = MeterProfile(
        modelId = "wave-energy-pwm-72",
        displayName = "Wave Energy-PWM-72",
        slaveId = 2,
        baudRate = 9600,
        dataBits = 8,
        parity = 0,
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            MeterPoint("A-B線電圧", 203, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.LINE_VOLTAGE_AB),
            MeterPoint("B-C線電圧", 204, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.LINE_VOLTAGE_BC),
            MeterPoint("C-A線電圧", 205, 1, 1.0, DataType.INT, "V", 6600, signalType = SignalType.LINE_VOLTAGE_CA),
            MeterPoint("A相電流", 206, 1, 1.0, DataType.INT, "A", 4, signalType = SignalType.PHASE_A_CURRENT),
            MeterPoint("B相電流", 207, 1, 1.0, DataType.INT, "A", 5, signalType = SignalType.PHASE_B_CURRENT),
            MeterPoint("C相電流", 208, 1, 1.0, DataType.INT, "A", 6, signalType = SignalType.PHASE_C_CURRENT),
            MeterPoint("有効電力", 209, 1, 1.0, DataType.INT, "kW", 94, signalType = SignalType.ACTIVE_POWER_TOTAL),
            MeterPoint("正方向合計有効電力量", 210, 2, 1.0, DataType.INT, "kWh", 1235, signalType = SignalType.FORWARD_ACTIVE_ENERGY_TOTAL),
            MeterPoint("負方向合計有効電力量", 212, 2, 1.0, DataType.INT, "kWh", 1, signalType = SignalType.REVERSE_ACTIVE_ENERGY_TOTAL),
            MeterPoint("無効電力", 214, 1, 1.0, DataType.INT, "kVar", 31, signalType = SignalType.REACTIVE_POWER_TOTAL)
        )
    )
}

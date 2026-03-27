package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

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
            MeterPoint("A Phase Voltage", 37101, 2, 10.0, DataType.INT, "V", 1010),
            MeterPoint("B Phase Voltage", 37103, 2, 10.0, DataType.INT, "V", 1010),
            MeterPoint("A Phase Current", 37107, 2, 100.0, DataType.INT, "A", 500),
            MeterPoint("B Phase Current", 37109, 2, 100.0, DataType.INT, "A", 400),
            MeterPoint("Active Power", 37113, 2, 1000.0, DataType.INT, "kW", 863550),
            MeterPoint("Reactive Power", 37115, 2, 1000.0, DataType.INT, "kVar", 283600),
            MeterPoint("Power Factor", 37117, 2, 1000.0, DataType.INT, "", 950),
            MeterPoint("Forward Active Energy Total", 37119, 2, 100.0, DataType.INT, "kWh", 12345),
            MeterPoint("Reverse Active Energy Total", 37121, 2, 100.0, DataType.INT, "kWh", 4321),
            MeterPoint("Reactive Energy Total", 37123, 2, 100.0, DataType.INT, "kVarh", 111100),
            MeterPoint("A-B Line Voltage", 37126, 2, 10.0, DataType.INT, "V", 2020),
            MeterPoint("B-C Line Voltage", 37128, 2, 10.0, DataType.INT, "V", 1010),
            MeterPoint("C-A Line Voltage", 37130, 2, 10.0, DataType.INT, "V", 1010),
            MeterPoint("A Phase Active Power", 37132, 2, 1000.0, DataType.INT, "kW", 479750),
            MeterPoint("B Phase Active Power", 37134, 2, 1000.0, DataType.INT, "kW", 383800)
        )
    )
}

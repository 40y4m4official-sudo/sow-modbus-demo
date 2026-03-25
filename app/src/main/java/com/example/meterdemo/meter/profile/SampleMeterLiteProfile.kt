package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

object SampleMeterLiteProfile {
    val profile = MeterProfile(
        modelId = "sample-meter-lite",
        displayName = "Sample Meter Lite",
        slaveId = 5,
        baudRate = 19200,
        dataBits = 8,
        parity = 2,
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            MeterPoint("Phase A Current", 768, 1, 1.0, DataType.INT16, "A", 12),
            MeterPoint("Phase B Current", 769, 1, 1.0, DataType.INT16, "A", 11),
            MeterPoint("Phase C Current", 770, 1, 1.0, DataType.INT16, "A", 13),
            MeterPoint("Line Voltage A-B", 778, 1, 1.0, DataType.INT16, "V", 200),
            MeterPoint("Line Voltage B-C", 779, 1, 1.0, DataType.INT16, "V", 199),
            MeterPoint("Line Voltage C-A", 780, 1, 1.0, DataType.INT16, "V", 201),
            MeterPoint("Active Power", 794, 1, 1.0, DataType.INT16, "W", 3100),
            MeterPoint("Import Active Energy Total", 1304, 1, 100.0, DataType.INT16, "kWh", 8765),
            MeterPoint("Export Active Energy Total", 1306, 1, 100.0, DataType.INT16, "kWh", 43)
        )
    )
}

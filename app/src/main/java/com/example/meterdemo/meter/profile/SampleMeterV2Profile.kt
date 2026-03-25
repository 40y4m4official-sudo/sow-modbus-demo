package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

object SampleMeterV2Profile {
    val profile = MeterProfile(
        modelId = "sample-meter-v2",
        displayName = "Sample Power Meter V2",
        slaveId = 3,
        baudRate = 19200,
        dataBits = 8,
        parity = 2,
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            MeterPoint("Phase A Current", 768, 1, 10, DataType.INT16, "A", 152),
            MeterPoint("Phase B Current", 769, 1, 10, DataType.INT16, "A", 149),
            MeterPoint("Phase C Current", 770, 1, 10, DataType.INT16, "A", 154),
            MeterPoint("Line Voltage A-B", 778, 1, 1, DataType.INT16, "V", 208),
            MeterPoint("Line Voltage B-C", 779, 1, 1, DataType.INT16, "V", 209),
            MeterPoint("Line Voltage C-A", 780, 1, 1, DataType.INT16, "V", 207),
            MeterPoint("Active Power", 794, 1, 1, DataType.INT16, "W", 4980),
            MeterPoint("Import Active Energy Total", 1304, 1, 100, DataType.INT16, "kWh", 24567),
            MeterPoint("Export Active Energy Total", 1306, 1, 100, DataType.INT16, "kWh", 132)
        )
    )
}

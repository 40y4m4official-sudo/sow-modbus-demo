package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

object SampleMeterV1Profile {
    val profile = MeterProfile(
        modelId = "sample-meter-v1",
        displayName = "Sample Power Meter V1",
        slaveId = 2,
        baudRate = 19200,
        dataBits = 8,
        parity = 2, // Even parity. Map this to the USB serial library constant later.
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            MeterPoint(
                name = "Phase A Current",
                address = 768,
                registerCount = 1,
                gain = 1.0,
                dataType = DataType.INT16,
                unit = "A",
                initialRawValue = 15
            ),
            MeterPoint(
                name = "Phase B Current",
                address = 769,
                registerCount = 1,
                gain = 1.0,
                dataType = DataType.INT16,
                unit = "A",
                initialRawValue = 16
            ),
            MeterPoint(
                name = "Phase C Current",
                address = 770,
                registerCount = 1,
                gain = 1.0,
                dataType = DataType.INT16,
                unit = "A",
                initialRawValue = 14
            ),
            MeterPoint(
                name = "Line Voltage A-B",
                address = 778,
                registerCount = 1,
                gain = 1.0,
                dataType = DataType.INT16,
                unit = "V",
                initialRawValue = 210
            ),
            MeterPoint(
                name = "Line Voltage B-C",
                address = 779,
                registerCount = 1,
                gain = 1.0,
                dataType = DataType.INT16,
                unit = "V",
                initialRawValue = 211
            ),
            MeterPoint(
                name = "Line Voltage C-A",
                address = 780,
                registerCount = 1,
                gain = 1.0,
                dataType = DataType.INT16,
                unit = "V",
                initialRawValue = 209
            ),
            MeterPoint(
                name = "Active Power",
                address = 794,
                registerCount = 1,
                gain = 1.0,
                dataType = DataType.INT16,
                unit = "W",
                initialRawValue = 5200
            ),
            MeterPoint(
                name = "Import Active Energy Total",
                address = 1304,
                registerCount = 1,
                gain = 100.0,
                dataType = DataType.INT16,
                unit = "kWh",
                initialRawValue = 12345
            ),
            MeterPoint(
                name = "Export Active Energy Total",
                address = 1306,
                registerCount = 1,
                gain = 100.0,
                dataType = DataType.INT16,
                unit = "kWh",
                initialRawValue = 67
            )
        )
    )
}

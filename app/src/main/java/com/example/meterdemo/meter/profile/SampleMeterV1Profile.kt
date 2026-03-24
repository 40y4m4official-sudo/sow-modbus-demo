package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

object SampleMeterV1Profile {
    val profile = MeterProfile(
        modelId = "sample-meter-v1",
        displayName = "サンプル電力量計 V1",
        slaveId = 2,
        baudRate = 19200,
        dataBits = 8,
        parity = 2, // Even parity. USB-RS485 実装時は使用ライブラリの定数へ合わせる。
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            MeterPoint(
                name = "A相電流",
                address = 768,
                registerCount = 1,
                gain = 1,
                dataType = DataType.INT16,
                unit = "A",
                initialRawValue = 15
            ),
            MeterPoint(
                name = "B相電流",
                address = 769,
                registerCount = 1,
                gain = 1,
                dataType = DataType.INT16,
                unit = "A",
                initialRawValue = 16
            ),
            MeterPoint(
                name = "C相電流",
                address = 770,
                registerCount = 1,
                gain = 1,
                dataType = DataType.INT16,
                unit = "A",
                initialRawValue = 14
            ),
            MeterPoint(
                name = "A-B線電圧",
                address = 778,
                registerCount = 1,
                gain = 1,
                dataType = DataType.INT16,
                unit = "V",
                initialRawValue = 210
            ),
            MeterPoint(
                name = "B-C線電圧",
                address = 779,
                registerCount = 1,
                gain = 1,
                dataType = DataType.INT16,
                unit = "V",
                initialRawValue = 211
            ),
            MeterPoint(
                name = "C-A線電圧",
                address = 780,
                registerCount = 1,
                gain = 1,
                dataType = DataType.INT16,
                unit = "V",
                initialRawValue = 209
            ),
            MeterPoint(
                name = "有効電力",
                address = 794,
                registerCount = 1,
                gain = 1,
                dataType = DataType.INT16,
                unit = "W",
                initialRawValue = 5200
            ),
            MeterPoint(
                name = "正方向有効電力量合計値",
                address = 1304,
                registerCount = 1,
                gain = 100,
                dataType = DataType.INT16,
                unit = "kWh",
                initialRawValue = 12345
            ),
            MeterPoint(
                name = "負方向有効電力量合計値",
                address = 1306,
                registerCount = 1,
                gain = 100,
                dataType = DataType.INT16,
                unit = "kWh",
                initialRawValue = 67
            )
        )
    )
}

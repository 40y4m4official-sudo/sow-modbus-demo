package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

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
            MeterPoint("A相電圧", 782, 1, 1.0, DataType.INT, "V", 6600),
            MeterPoint("B相電圧", 783, 1, 1.0, DataType.INT, "V", 6600),
            MeterPoint("C相電圧", 784, 1, 1.0, DataType.INT, "V", 6600),
            MeterPoint("A-B線電圧", 778, 1, 1.0, DataType.INT, "V", 6600),
            MeterPoint("B-C線電圧", 779, 1, 1.0, DataType.INT, "V", 6600),
            MeterPoint("C-A線電圧", 780, 1, 1.0, DataType.INT, "V", 6600),
            MeterPoint("A相電流", 768, 1, 1.0, DataType.INT, "A", 1),
            MeterPoint("B相電流", 769, 1, 1.0, DataType.INT, "A", 2),
            MeterPoint("C相電流", 770, 1, 1.0, DataType.INT, "A", 3),
            MeterPoint("有効電力", 794, 1, 1.0, DataType.INT, "kW", 38),
            MeterPoint("A相有効電力", 791, 1, 1.0, DataType.INT, "kW", 6),
            MeterPoint("B相有効電力", 792, 1, 1.0, DataType.INT, "kW", 13),
            MeterPoint("C相有効電力", 793, 1, 1.0, DataType.INT, "kW", 19),
            MeterPoint("無効電力", 802, 1, 1.0, DataType.INT, "kVar", 12),
            MeterPoint("皮相電力", 806, 1, 1.0, DataType.INT, "kVA", 4),
            MeterPoint("正方向合計有効電力量", 1304, 2, 1.0, DataType.INT, "kWh", 1235),
            MeterPoint("負方向合計有効電力量", 1306, 2, 1.0, DataType.INT, "kWh", 1)
        )
    )
}

package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

object BackUpCtProfile {
    val profile = MeterProfile(
        modelId = "backup-ct",
        displayName = "BackUp-CT",
        slaveId = 2,
        baudRate = 19200,
        dataBits = 8,
        parity = 2,
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            MeterPoint("A相電圧", 37101, 2, 10.0, DataType.INT32, "V", 1010),
            MeterPoint("B相電圧", 37103, 2, 10.0, DataType.INT32, "V", 1010),
            MeterPoint("A相電流", 37107, 2, 100.0, DataType.INT32, "A", 500),
            MeterPoint("B相電流", 37109, 2, 100.0, DataType.INT32, "A", 400),
            MeterPoint("有効電力", 37113, 2, 1000.0, DataType.INT32, "kW", 863550),
            MeterPoint("無効電力", 37115, 2, 1000.0, DataType.INT32, "kVar", 283600),
            MeterPoint("力率", 37117, 1, 1000.0, DataType.INT16, "", 950),
            MeterPoint("正方向合計有効電力量", 37119, 2, 100.0, DataType.INT32, "kWh", 12345),
            MeterPoint("負方向合計有効電力量", 37121, 2, 100.0, DataType.INT32, "kWh", 4321),
            MeterPoint("合計無効電力量", 37123, 2, 100.0, DataType.INT32, "kVarh", 111100),
            MeterPoint("A-B線電圧", 37126, 2, 10.0, DataType.INT32, "V", 2020),
            MeterPoint("B-C線電圧", 37128, 2, 10.0, DataType.INT32, "V", 1010),
            MeterPoint("C-A線電圧", 37130, 2, 10.0, DataType.INT32, "V", 1010),
            MeterPoint("A相有効電力", 37132, 2, 1000.0, DataType.INT32, "kW", 479750),
            MeterPoint("B相有効電力", 37134, 2, 1000.0, DataType.INT32, "kW", 383800)
        )
    )
}

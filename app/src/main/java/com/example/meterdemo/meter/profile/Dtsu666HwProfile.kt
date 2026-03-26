package com.example.meterdemo.meter.profiles

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile

object Dtsu666HwProfile {
    val profile = MeterProfile(
        modelId = "dtsu666-hw",
        displayName = "DTSU666-HW",
        slaveId = 2,
        baudRate = 9600,
        dataBits = 8,
        parity = 0,
        stopBits = 1,
        functionCode = 0x03,
        points = listOf(
            floatPoint("A相電圧", 2110, 1.0, "V", 6600.0),
            floatPoint("B相電圧", 2112, 1.0, "V", 6600.0),
            floatPoint("C相電圧", 2114, 1.0, "V", 6600.0),
            floatPoint("A-B線電圧", 2118, 1.0, "V", 6600.0),
            floatPoint("B-C線電圧", 2120, 1.0, "V", 6600.0),
            floatPoint("C-A線電圧", 2122, 1.0, "V", 6600.0),
            floatPoint("A相電流", 2102, 1.0, "A", 1.0),
            floatPoint("B相電流", 2104, 1.0, "A", 2.0),
            floatPoint("C相電流", 2106, 1.0, "A", 3.0),
            floatPoint("有効電力", 2264, 1000.0, "kW", 37.62),
            floatPoint("A相有効電力", 2266, 1000.0, "kW", 6.27),
            floatPoint("B相有効電力", 2268, 1000.0, "kW", 12.54),
            floatPoint("C相有効電力", 2270, 1000.0, "kW", 18.81),
            floatPoint("無効電力", 2272, 1000.0, "kVar", 12.35),
            floatPoint("皮相電力", 2142, 1000.0, "kVA", 3.96),
            floatPoint("合計有効電力量", 2158, 1.0, "kWh", 0.0),
            floatPoint("合計無効電力量", 2206, 1.0, "kVarh", 0.0),
            floatPoint("正方向合計有効電力量", 2166, 1.0, "kWh", 1234.5),
            floatPoint("正方向合計無効電力量", 2214, 1.0, "kVarh", 0.0),
            floatPoint("負方向合計有効電力量", 2174, 1.0, "kWh", 1.23),
            floatPoint("負方向合計無効電力量", 2222, 1.0, "kVarh", 0.0)
        )
    )

    private fun floatPoint(
        name: String,
        address: Int,
        gain: Double,
        unit: String,
        displayValue: Double
    ): MeterPoint {
        val rawValue = if (gain <= 1.0) {
            displayValue.toFloat()
        } else {
            (displayValue * gain).toFloat()
        }

        return MeterPoint(
            name = name,
            address = address,
            registerCount = 2,
            gain = gain,
            dataType = DataType.FLOAT,
            unit = unit,
            initialRawValue = rawValue.toRawBits()
        )
    }
}

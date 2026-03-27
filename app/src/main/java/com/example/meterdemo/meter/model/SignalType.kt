package com.example.meterdemo.meter.model

enum class SignalType(val label: String, val defaultUnit: String) {
    PHASE_A_VOLTAGE("A相電圧", "V"),
    PHASE_B_VOLTAGE("B相電圧", "V"),
    PHASE_C_VOLTAGE("C相電圧", "V"),
    LINE_VOLTAGE_AB("A-B線電圧", "V"),
    LINE_VOLTAGE_BC("B-C線電圧", "V"),
    LINE_VOLTAGE_CA("C-A線電圧", "V"),
    PHASE_A_CURRENT("A相電流", "A"),
    PHASE_B_CURRENT("B相電流", "A"),
    PHASE_C_CURRENT("C相電流", "A"),
    ACTIVE_POWER_TOTAL("有効電力", "kW"),
    PHASE_A_ACTIVE_POWER("A相有効電力", "kW"),
    PHASE_B_ACTIVE_POWER("B相有効電力", "kW"),
    PHASE_C_ACTIVE_POWER("C相有効電力", "kW"),
    REACTIVE_POWER_TOTAL("無効電力", "kVar"),
    POWER_FACTOR("力率", ""),
    APPARENT_POWER_TOTAL("皮相電力", "kVA"),
    TOTAL_ACTIVE_ENERGY("合計有効電力量", "kWh"),
    TOTAL_REACTIVE_ENERGY("合計無効電力量", "kVarh"),
    FORWARD_ACTIVE_ENERGY_TOTAL("正方向合計有効電力量", "kWh"),
    FORWARD_REACTIVE_ENERGY_TOTAL("正方向合計無効電力量", "kVarh"),
    REVERSE_ACTIVE_ENERGY_TOTAL("負方向合計有効電力量", "kWh"),
    REVERSE_REACTIVE_ENERGY_TOTAL("負方向合計無効電力量", "kVarh"),
    CUSTOM("カスタム", "");

    companion object {
        fun fromStoredName(name: String): SignalType {
            return entries.firstOrNull { it.name == name } ?: CUSTOM
        }
    }
}

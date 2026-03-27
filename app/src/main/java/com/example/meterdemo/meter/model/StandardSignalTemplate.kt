package com.example.meterdemo.meter.model

data class StandardSignalTemplate(
    val signalType: SignalType
)

object StandardSignalTemplates {
    val all: List<StandardSignalTemplate> = listOf(
        StandardSignalTemplate(SignalType.PHASE_A_VOLTAGE),
        StandardSignalTemplate(SignalType.PHASE_B_VOLTAGE),
        StandardSignalTemplate(SignalType.PHASE_C_VOLTAGE),
        StandardSignalTemplate(SignalType.LINE_VOLTAGE_AB),
        StandardSignalTemplate(SignalType.LINE_VOLTAGE_BC),
        StandardSignalTemplate(SignalType.LINE_VOLTAGE_CA),
        StandardSignalTemplate(SignalType.PHASE_A_CURRENT),
        StandardSignalTemplate(SignalType.PHASE_B_CURRENT),
        StandardSignalTemplate(SignalType.PHASE_C_CURRENT),
        StandardSignalTemplate(SignalType.ACTIVE_POWER_TOTAL),
        StandardSignalTemplate(SignalType.PHASE_A_ACTIVE_POWER),
        StandardSignalTemplate(SignalType.PHASE_B_ACTIVE_POWER),
        StandardSignalTemplate(SignalType.PHASE_C_ACTIVE_POWER),
        StandardSignalTemplate(SignalType.REACTIVE_POWER_TOTAL),
        StandardSignalTemplate(SignalType.POWER_FACTOR),
        StandardSignalTemplate(SignalType.APPARENT_POWER_TOTAL),
        StandardSignalTemplate(SignalType.TOTAL_ACTIVE_ENERGY),
        StandardSignalTemplate(SignalType.TOTAL_REACTIVE_ENERGY),
        StandardSignalTemplate(SignalType.FORWARD_ACTIVE_ENERGY_TOTAL),
        StandardSignalTemplate(SignalType.FORWARD_REACTIVE_ENERGY_TOTAL),
        StandardSignalTemplate(SignalType.REVERSE_ACTIVE_ENERGY_TOTAL),
        StandardSignalTemplate(SignalType.REVERSE_REACTIVE_ENERGY_TOTAL)
    )
}

package com.example.meterdemo.meter.model

data class StandardSignalTemplate(
    val name: String,
    val unit: String
)

object StandardSignalTemplates {
    val all: List<StandardSignalTemplate> = listOf(
        StandardSignalTemplate("Phase A Voltage", "V"),
        StandardSignalTemplate("Phase B Voltage", "V"),
        StandardSignalTemplate("Phase C Voltage", "V"),
        StandardSignalTemplate("Line Voltage A-B", "V"),
        StandardSignalTemplate("Line Voltage B-C", "V"),
        StandardSignalTemplate("Line Voltage C-A", "V"),
        StandardSignalTemplate("Phase A Current", "A"),
        StandardSignalTemplate("Phase B Current", "A"),
        StandardSignalTemplate("Phase C Current", "A"),
        StandardSignalTemplate("Active Power", "kW"),
        StandardSignalTemplate("Phase A Active Power", "kW"),
        StandardSignalTemplate("Phase B Active Power", "kW"),
        StandardSignalTemplate("Phase C Active Power", "kW"),
        StandardSignalTemplate("Reactive Power", "kVar"),
        StandardSignalTemplate("Power Factor", ""),
        StandardSignalTemplate("Apparent Power", "kVA"),
        StandardSignalTemplate("Total Active Energy", "kWh"),
        StandardSignalTemplate("Total Reactive Energy", "kVarh"),
        StandardSignalTemplate("Import Active Energy Total", "kWh"),
        StandardSignalTemplate("Import Reactive Energy Total", "kVarh"),
        StandardSignalTemplate("Export Active Energy Total", "kWh"),
        StandardSignalTemplate("Export Reactive Energy Total", "kVarh")
    )
}

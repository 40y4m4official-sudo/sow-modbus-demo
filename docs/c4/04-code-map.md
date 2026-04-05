# Code Map

## Purpose

Map conceptual components to concrete code locations.

## Mapping

### Entry Point

- `app/src/main/java/com/example/meterdemo/MainActivity.kt`

### App Composition / Navigation

- `app/src/main/java/com/example/meterdemo/ui/MeterDemoApp.kt`

### Main State / Orchestration

- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewMode.kt`

### UI Screens

- `app/src/main/java/com/example/meterdemo/ui/MeterValuesScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/SettingsScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/LogsScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/LogSummaryScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/EditMeterScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/AddMeterScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/ScreenHeader.kt`

### Meter Domain Model

- `app/src/main/java/com/example/meterdemo/meter/model/SignalType.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/MeterPoint.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/MeterProfile.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/StandardSignalTemplate.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/WordByteOrder.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/SerialParity.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/DataType.kt`

### Built-in Presets

- `app/src/main/java/com/example/meterdemo/meter/profile/BackUpCtProfile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/MitsubishiMe110SsrMbProfile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/Dtsu666HwProfile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/YadaYds60_80Profile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/WaveEnergyPwm72Profile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/Drpr72Dvrr72Profile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/MeterProfiles.kt`

### Repository / Value Handling

- `app/src/main/java/com/example/meterdemo/meter/repository/MeterRepository.kt`

### Simulation

- `app/src/main/java/com/example/meterdemo/meter/simulation/MeterSimulationEngine.kt`

### Modbus

- `app/src/main/java/com/example/meterdemo/modbus/ModbusRtuSlaveEngine.kt`
- `app/src/main/java/com/example/meterdemo/modbus/ModbusFrameParser.kt`
- `app/src/main/java/com/example/meterdemo/modbus/ModbusCrc.kt`

### USB / Serial

- `app/src/main/java/com/example/meterdemo/usb/UsbDeviceScanner.kt`
- `app/src/main/java/com/example/meterdemo/usb/UsbSerialScanner.kt`
- `app/src/main/java/com/example/meterdemo/usb/UsbSerialConnectionManager.kt`
- `app/src/main/java/com/example/meterdemo/usb/UsbRequestFrameAssembler.kt`

### Logging

- `app/src/main/java/com/example/meterdemo/logging/CommLog.kt`
- `app/src/main/java/com/example/meterdemo/logging/CommLogger.kt`
- `app/src/main/java/com/example/meterdemo/logging/LogExporter.kt`
- `app/src/main/java/com/example/meterdemo/logging/LogAddressSummaryAnalyzer.kt`

### Persistence

- `app/src/main/java/com/example/meterdemo/persistence/MeterPersistence.kt`

### Build / Release / Update

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app-update.json`
- `docs/APP_UPDATE_RELEASE_FLOW.md`
- `AGENTS.md`

### Tests

- `app/src/test/java/com/example/meterdemo/modbus/*`
- `app/src/test/java/com/example/meterdemo/meter/repository/*`
- `app/src/test/java/com/example/meterdemo/usb/*`
- `app/src/test/java/com/example/meterdemo/logging/*`

## Traceability Rule

When a feature changes:
- update its implementation files
- update the matching `docs/specs/*.md`
- update the affected `docs/c4/*.md` section if structure or responsibilities changed

# Component View - Android App

## Purpose

Describe the main internal components of the Android application.

## Major Components

### MainActivity

- File:
  - `app/src/main/java/com/example/meterdemo/MainActivity.kt`
- Responsibilities:
  - Android entry point
  - creates Compose content
  - hosts `MeterDemoApp`

### MeterDemoApp

- File:
  - `app/src/main/java/com/example/meterdemo/ui/MeterDemoApp.kt`
- Responsibilities:
  - top-level screen composition and navigation
  - wires screen callbacks to `MainViewModel`

### UI Screens

- Files:
  - `app/src/main/java/com/example/meterdemo/ui/*`
- Responsibilities:
  - render Main, Settings, Logs, Summary, Edit Meter, Add Meter
  - present current state from `MainUiState`
  - collect user input and invoke view model actions

### MainViewModel

- File:
  - `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- Responsibilities:
  - main state holder for UI
  - orchestrates repository, Modbus engine, USB, simulation, persistence, logging, updates, and language selection
  - converts display input to raw values
  - restores persisted state on startup
  - owns update check/download/install flow

### MeterRepository

- File:
  - `app/src/main/java/com/example/meterdemo/meter/repository/MeterRepository.kt`
- Responsibilities:
  - holds active meter profile
  - stores current raw values by register start address
  - returns formatted snapshots for UI
  - computes active address range from configured points
  - provides zero-fill eligibility decisions for undefined addresses inside active range

### ModbusRtuSlaveEngine

- File:
  - `app/src/main/java/com/example/meterdemo/modbus/ModbusRtuSlaveEngine.kt`
- Responsibilities:
  - validate RTU request frames
  - enforce current slave ID and function code
  - build normal responses and exception responses
  - encode values using register count and word/byte order
  - zero-fill undefined addresses inside active range

### USB Serial Integration

- Files:
  - `app/src/main/java/com/example/meterdemo/usb/*`
- Responsibilities:
  - detect USB and USB-serial devices
  - request USB permission
  - open FTDI-compatible serial port with profile communication settings
  - receive fragmented bytes
  - reassemble full RTU request frames
  - write slave responses back to the serial port

### MeterSimulationEngine

- File:
  - `app/src/main/java/com/example/meterdemo/meter/simulation/MeterSimulationEngine.kt`
- Responsibilities:
  - maintain simulation-only floating-point state
  - update voltages, currents, power factor, power, and energy values
  - preserve formula-driven relationships such as `V x I x PF`
  - keep three-phase current movement cohesive via shared base current logic

### MeterPersistence

- File:
  - `app/src/main/java/com/example/meterdemo/persistence/MeterPersistence.kt`
- Responsibilities:
  - persist user profiles
  - persist selected profile, slave ID, raw values, and main view mode

### Localization

- Files:
  - `app/src/main/java/com/example/meterdemo/localization/*`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ja/strings.xml`
- Responsibilities:
  - store current app language selection
  - apply stored locale on app startup
  - expose current language to settings UI
  - provide localized UI strings for supported languages

### CommLogger / Logging Components

- Files:
  - `app/src/main/java/com/example/meterdemo/logging/*`
- Responsibilities:
  - store categorized logs in memory
  - export logs to file
  - summarize USB RX request patterns by start address and quantity

### App Update Flow

- Main location:
  - `MainViewModel`
- Supporting pieces:
  - `FileProvider`
  - `app-update.json`
- Responsibilities:
  - fetch remote JSON metadata
  - compare installed version and latest version
  - download APK into app storage
  - launch installer or unknown-sources settings flow

## Main Relationships

- UI screens depend on `MainUiState` from `MainViewModel`.
- `MainViewModel` depends on:
  - `MeterRepository`
  - `ModbusRtuSlaveEngine`
  - `UsbSerialConnectionManager`
  - `UsbRequestFrameAssembler`
  - `MeterSimulationEngine`
  - `MeterPersistence`
  - `CommLogger`
  - `AppLanguageManager`
- `ModbusRtuSlaveEngine` depends on `MeterRepository` for actual register data.
- USB layer feeds validated request frames to `MainViewModel`, which passes them to `ModbusRtuSlaveEngine`.
- Simulation writes updated raw values back into `MeterRepository` through the view model.

## Architectural Notes

- `MainViewModel` is currently the main orchestrator and contains substantial coordination logic.
- The app update flow is intentionally implemented without Play in-app update APIs.
- Logging is in-memory first, with export on demand.
- Preset compatibility tuning is handled in profile definitions and repository/modbus behavior, not in a separate rules engine.

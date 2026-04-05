# System Context

## Purpose

Describe the overall system boundary of the SOW Modbus Demo app and its external actors.

## System

- System name:
  - `SOW Modbus Demo`
- Primary responsibility:
  - Android app that simulates electric meter behavior and responds as a Modbus RTU slave over USB-RS485.
- Main user value:
  - Reproduce meter presets, manual value control, simulated value movement, communication logging, and APK self-update without Google Play in-app updates.

## External Actors / Systems

### Operator

- Uses the Android UI to:
  - choose a built-in preset
  - add/edit user-defined meters
  - change displayed/current values
  - start/stop simulation
  - inspect logs and summaries
  - trigger APK update checks

### External Modbus Master

- Reads Modbus RTU registers from the Android device through RS485.
- Typical examples:
  - SmartLogger
  - QModMaster
  - other Modbus master equipment under test

### SmartLogger

- High-priority real-world compatibility target.
- Often performs block reads rather than single-register reads.
- Drives several design choices:
  - fragmented USB frame reassembly
  - zero-fill inside configured active address range
  - preset tuning by measured compatibility

### QModMaster

- PC-based manual Modbus master.
- Used mainly for:
  - direct register verification
  - preset debugging
  - comparing real device and app responses

### USB-RS485 Adapter

- Physical bridge between Android USB host and RS485 line.
- Current target adapter family:
  - FTDI-based devices such as DSD TECH SH-U11C

### Public Update Metadata

- Static JSON file hosted from the public repository.
- Tells the app:
  - latest `versionCode`
  - latest `versionName`
  - direct APK asset URL

### GitHub Releases

- Hosts public signed APK assets for download.
- Used instead of Google Play in-app update APIs.

## Key Relationships

1. Operator configures or edits a meter profile in the Android app.
2. External Modbus master polls the Android device through RS485.
3. The app returns register values from:
   - current preset
   - current raw values
   - simulation engine output
4. The app logs USB / Modbus activity and can export logs.
5. The app checks public update metadata, downloads a newer APK, and launches the installer through `FileProvider`.

## System Boundary Notes

Inside the app boundary:
- UI and navigation
- preset management
- user-defined meter storage
- Modbus slave engine
- USB serial integration
- simulation engine
- logging and export
- direct APK update flow

Outside the app boundary:
- external masters and test tools
- physical USB/RS485 hardware
- GitHub hosting for update metadata and release assets

## Current Assumptions

- Only Modbus RTU over USB-RS485 is supported.
- Supported read requests are profile-driven and limited to read-register functions.
- APK update distribution is public and does not require authentication.
- Release versioning is synchronized between Gradle version, `app-update.json`, and Git tag.

# Container View

## Purpose

Describe the high-level runtime containers involved in the system.

## Containers

### 1. Android Application

- Technology:
  - Kotlin
  - Jetpack Compose
  - Android USB host APIs
  - `usb-serial-for-android`
- Responsibilities:
  - render UI
  - manage presets and user meters
  - hold current raw values
  - act as a Modbus RTU slave
  - handle USB serial connection lifecycle
  - simulate values
  - persist state locally
  - export logs
  - check/download/install app updates

### 2. Local App Storage

- Technology:
  - app-private files
  - persisted preferences/state storage
- Responsibilities:
  - persist user profiles
  - persist selected profile and raw values
  - store downloaded update APK before install
  - store exported log files

### 3. Public Update Metadata

- Technology:
  - static JSON file served from GitHub raw content
- Responsibilities:
  - expose latest `versionCode`
  - expose latest `versionName`
  - expose direct APK asset URL

### 4. Public APK Asset Hosting

- Technology:
  - GitHub Releases
- Responsibilities:
  - host signed APK assets for direct download

### 5. External Master Systems

- Examples:
  - SmartLogger
  - QModMaster
- Responsibilities:
  - issue Modbus RTU read requests
  - interpret returned register values
  - validate compatibility of presets and value behavior

### 6. USB-RS485 Hardware Path

- Technology:
  - Android USB host + FTDI-compatible adapter + RS485 wiring
- Responsibilities:
  - carry serial bytes between Android app and external master device

## Data Flow Summary

1. Operator launches and configures the app.
2. External master sends Modbus RTU read requests over RS485.
3. USB serial layer receives bytes and reassembles complete frames.
4. Modbus engine validates request and builds a response from repository data.
5. Response is sent back over USB serial.
6. Logs are stored, summarized, and optionally exported.
7. For updates, the app fetches remote JSON, compares version, downloads a signed APK, and invokes installer flow.

## Container Boundaries

- The Android app is the only runtime container under direct code control.
- GitHub raw JSON and GitHub Releases are passive distribution containers.
- External master tools and USB hardware are integration dependencies, not owned containers.

## Operational Notes

- Release APKs are generated with a fixed naming rule:
  - `SOW-Modbus-Demo-vX.Y.Z-Release.apk`
- Update metadata is expected at:
  - `https://raw.githubusercontent.com/40y4m4official-sudo/sow-modbus-demo/main/app-update.json`
- Release assets are expected in public GitHub Releases under the same repository.

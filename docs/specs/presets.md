# Meter Preset Specification

## Scope

Document built-in meter presets and how they are maintained.

## Built-in Presets

Current built-in profiles:
- `BackUp-CT`
- `Mitsubishi-ME110SSR-MB`
- `DTSU666-HW`
- `YADA-YDS60-80`
- `Wave Energy-PWM-72`
- `DRPR-72/DVRR-72`

Default preset:
- `BackUp-CT`

## Common Preset Structure

Each `MeterProfile` contains:
- `modelId`
- `displayName`
- `slaveId`
- `baudRate`
- `dataBits`
- `parity`
- `stopBits`
- `functionCode`
- `points`

Each `MeterPoint` contains:
- fixed signal meaning via `SignalType`
- display name
- start address
- register count
- gain
- data type
- word/byte order
- initial raw value

## Communication Settings

### BackUp-CT

- Function code:
  - `0x03`
- Serial:
  - `9600 / 8N1`

### Mitsubishi-ME110SSR-MB

- Function code:
  - `0x03`
- Serial:
  - `19200 / 8E1`

### DTSU666-HW

- Function code:
  - `0x03`
- Serial:
  - `9600 / 8N1`

### YADA-YDS60-80

- Function code:
  - `0x03`
- Serial:
  - `9600 / 8N1`

### Wave Energy-PWM-72

- Function code:
  - `0x03`
- Serial:
  - `9600 / 8N1`

### DRPR-72/DVRR-72

- Function code:
  - `0x03`
- Serial:
  - `9600 / 8N1`

## Preset Editing Policy

- Built-in presets are read-only in the app
- User-added meters are editable in `Edit Meter`
- Standard 22 signal templates use fixed names and fixed `SignalType`
- Unused standard registers are represented by leaving the address blank
- Custom non-template registers can still be added with `Add Register`

## Standard 22 Signal Types

The standard template covers these fixed signal meanings:
- A���d��
- B���d��
- C���d��
- A-B���d��
- B-C���d��
- C-A���d��
- A���d��
- B���d��
- C���d��
- �L���d��
- A���L���d��
- B���L���d��
- C���L���d��
- �����d��
- �͗�
- �瑊�d��
- ���v�L���d�͗�
- ���v�����d�͗�
- ���������v�L���d�͗�
- ���������v�����d�͗�
- ���������v�L���d�͗�
- ���������v�����d�͗�

## Maintenance Rules

When changing a preset:
- preserve tested communication settings unless intentionally revalidated
- update initial values carefully when SmartLogger compatibility depends on them
- prefer zero-fill active-range behavior over hard-coded dummy points when gaps only exist to satisfy block reads
- keep `SignalType` aligned with intended simulation/derived behavior

## Verification Notes

Known validation tools:
- SmartLogger
- QModMaster

Typical verification points:
- communication settings
- block-read compatibility
- displayed value agreement with master tool
- handling of gaps inside active address range

## Related Code

- `app/src/main/java/com/example/meterdemo/meter/profile/*`
- `app/src/main/java/com/example/meterdemo/meter/model/SignalType.kt`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`

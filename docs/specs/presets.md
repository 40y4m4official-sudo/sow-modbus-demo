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
- A相電圧
- B相電圧
- C相電圧
- A-B線電圧
- B-C線電圧
- C-A線電圧
- A相電流
- B相電流
- C相電流
- 有効電力
- A相有効電力
- B相有効電力
- C相有効電力
- 無効電力
- 力率
- 皮相電力
- 合計有効電力量
- 合計無効電力量
- 正方向合計有効電力量
- 正方向合計無効電力量
- 負方向合計有効電力量
- 負方向合計無効電力量

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

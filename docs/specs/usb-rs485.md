# USB-RS485 Specification

## Scope

Document USB serial and RS485 communication behavior.

## Supported Adapter Assumption

Primary tested adapter family:
- FTDI-compatible USB serial adapters
- example target device:
  - DSD TECH SH-U11C

The app relies on `usb-serial-for-android` default probing.

## Discovery Flow

Settings screen provides:
- generic USB device refresh
- USB serial device refresh

Displayed device information includes:
- device label
- device path/name
- VID:PID
- permission state

## Permission Flow

- app requests USB permission per selected serial device
- permission result is reported through `UsbSerialConnectionManager.Listener`
- permission status is reflected in UI and logs

## Connection Flow

When user taps `Connect`:
1. selected profile communication settings are read
2. app attempts to open the selected serial port
3. serial parameters are applied:
   - baud rate from profile
   - 8 data bits
   - parity from profile
   - stop bits from profile
4. DTR and RTS are set true
5. `SerialInputOutputManager` is started

Disconnect may happen from:
- explicit user action
- reconnect path
- I/O run error
- view model clearing

## Serial Settings Source

Current serial settings come from the active meter profile:
- `baudRate`
- `parity`
- `stopBits`

Supported baud rates in editor/UI:
- `1200`
- `2400`
- `4800`
- `9600`
- `19200`
- `115200`

Supported parity values:
- None
- Odd
- Even

Supported stop bits:
- 1
- 2

## Frame Reassembly Rules

USB reads may arrive fragmented or with noise.

`UsbRequestFrameAssembler` behavior:
- accumulates incoming bytes in a buffer
- scans for first valid 8-byte Modbus RTU request frame
- validates:
  - expected slave ID
  - allowed function code
  - CRC
  - request parser success
- emits complete frames
- drops leading noise when needed
- preserves likely frame start bytes near buffer tail for future completion

Allowed request functions at assembly stage:
- `0x03`
- `0x04`

## Runtime Handling in ViewModel

For each received USB chunk:
1. raw bytes are logged as `USB / RX`
2. bytes are appended into frame assembler
3. dropped noise is logged as informational USB log
4. each completed frame is passed to `ModbusRtuSlaveEngine`
5. if response exists, response is written back to USB and logged as `USB / TX`
6. if request is not handled, an error log entry is created

## Error Handling

Typical error states logged by the app:
- permission denied
- device not found
- failed to open USB device
- no serial port found
- connect failed
- write failed
- I/O stopped
- request not handled

## Limitations

- no background Android service for persistent USB communication
- no multi-device concurrent serial sessions
- current integration targets read-only Modbus slave behavior

## Related Code

- `app/src/main/java/com/example/meterdemo/usb/*`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/meterdemo/modbus/*`

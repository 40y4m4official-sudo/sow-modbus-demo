# Modbus Specification

## Scope

Document supported Modbus RTU behavior of the Android app acting as a slave.

## Supported Request Types

- Read registers only
- Supported function codes depend on the active profile:
  - `0x03` Read Holding Registers
  - `0x04` Read Input Registers
- The active profile exposes exactly one function code at a time.
- If the incoming function code does not match the active profile function code, the app returns:
  - `Illegal Function (0x01)`

## Request Validation Rules

A request is ignored or rejected as follows.

### Frame ignored (`null` response)

- frame size is less than 8 bytes
- CRC is invalid
- slave ID does not match the currently configured slave ID

### Exception response returned

- function code does not match current profile:
  - `Illegal Function (0x01)`
- quantity is less than or equal to 0:
  - `Illegal Data Value (0x03)`
- address/quantity cannot be satisfied under current profile rules:
  - `Illegal Data Address (0x02)`

## Addressing Rules

- Register values are modeled per `MeterPoint.address`
- `MeterPoint.registerCount` may be `1..4`
- The repository computes an active address range automatically:
  - `min(point.address)` to `max(point.address + registerCount - 1)`
- Requests inside this range are treated differently from requests outside it

## Zero-Fill Behavior Inside Active Range

If a master reads an address inside the active address range that has no explicit `MeterPoint` starting there:
- the app returns `0x0000` for that register
- this supports block-reading masters such as SmartLogger

If a request goes outside the active address range:
- the app returns `Illegal Data Address (0x02)`

## Multi-Register Read Behavior

- The engine builds a response sequentially from `startAddress` for `quantity` registers.
- If a defined point starts at the current address:
  - its full encoded value is appended
  - `registerCount` registers are consumed
- If no point starts at the current address but the address is inside active range:
  - one zero register is appended
  - one register is consumed
- If a point would overflow the requested quantity:
  - the request is rejected with `Illegal Data Address (0x02)`

## Data Encoding Rules

### Integer

- `DataType.INT`
- Encoded as signed-extended or zero-extended raw integer into `registerCount * 2` bytes
- For 1-register values, practical raw write range in UI is:
  - `-32768..65535`

### Float

- `DataType.FLOAT`
- Encoded from 32-bit float bits stored in raw value
- Currently meaningful with `registerCount = 2`

## Word / Byte Order

Supported orders:
- `MSB+MSB`
- `LSB+LSB`
- `MSB+LSB`
- `LSB+MSB`

Encoding flow:
1. Create natural-order bytes for the required register count
2. Split into 16-bit words
3. Reverse word order when configured for `LSB_*`
4. Reverse bytes inside each word when configured for `*_LSB`

## Slave ID Handling

- Valid slave ID range in UI and repository:
  - `1..247`
- Incoming request must match current slave ID or it is ignored silently

## Compatibility Notes

- SmartLogger often performs wide block reads instead of single-register reads
- The active-range zero-fill rule exists mainly to support these reads without forcing explicit dummy points for every gap
- Request fragmentation on USB serial is handled before Modbus parsing by `UsbRequestFrameAssembler`

## Related Code

- `app/src/main/java/com/example/meterdemo/modbus/*`
- `app/src/main/java/com/example/meterdemo/meter/repository/*`
- `app/src/main/java/com/example/meterdemo/usb/UsbRequestFrameAssembler.kt`

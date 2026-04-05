# Logging Specification

## Scope

Document logging, export, and summary behavior.

## Log Model

Each log entry stores:
- category
- direction
- HEX payload when applicable
- note/message
- timestamp

## Categories

Current categories:
- `SYSTEM`
- `MODBUS`
- `USB`

## Directions

Current directions:
- `RX`
- `TX`
- `INFO`
- `ERROR`

## Retention Policy

- logs are stored in memory through `CommLogger`
- max retained entries:
  - `5000`
- newest entries are inserted at the front
- old entries are dropped once capacity is exceeded

## Logging Sources

Typical sources include:
- manual Comm Test requests/responses
- USB RX/TX traffic
- USB permission/connect/disconnect events
- dropped USB noise notices
- simulation start/stop
- preset changes
- slave ID updates
- value apply/reset events
- app update status messages

## Export Behavior

- export target directory:
  - app external files dir, fallback to app files dir
  - under `log_exports/`
- export file name:
  - `meter_demo_logs_yyyyMMdd_HHmmss.txt`
- file is shared via `FileProvider`

### Export Line Format

Each exported line contains:
- timestamp
- category
- direction
- optional HEX payload
- optional note

Example shape:
- `[2026-03-27 12:54:02.351] USB / RX / 02 03 90 F9 00 04 B9 0B | USB RX`

## Summary Aggregation

Summary is derived from USB RX logs only.

Aggregation keys:
- slave ID
- function code
- start address
- quantity

The analyzer:
- reparses fragmented USB RX byte streams
- reconstructs valid 8-byte RTU read requests
- groups by request signature
- reports count, latest timestamp, and sample HEX

This summary is intended to answer questions such as:
- which start addresses SmartLogger is actually reading
- how often a request pattern occurs
- whether a suspected address is ever requested

## Interpretation Rules

- raw USB chunks may not match complete RTU frames
- summary analysis is more reliable than visual inspection of high-rate raw logs
- `Dropped USB noise` entries indicate bytes discarded during frame resynchronization

## Current Limitations

- logs are not persisted across app restarts unless manually exported
- filters are basic and primarily screen-level
- summary currently focuses on 8-byte read requests

## Related Code

- `app/src/main/java/com/example/meterdemo/logging/*`
- `app/src/main/java/com/example/meterdemo/ui/LogsScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/LogSummaryScreen.kt`
- `app/src/main/java/com/example/meterdemo/usb/UsbRequestFrameAssembler.kt`

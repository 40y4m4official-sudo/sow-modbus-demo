# UI Specification

## Scope

Document the screen structure, major flows, and current UI rules.

## Screen List

- Main
- Settings
- Edit Meter
- Add Meter / Register Settings
- Logs
- Log Summary

## Main Screen

### Main Modes

Supported display modes:
- Card View
- List View

#### Card View

- one selected signal at a time
- previous/next controls
- large value panel
- manual value input for selected point
- auto simulation start/stop controls

#### List View

- table-like layout
- columns:
  - `Addr`
  - `Item`
  - `Value`
- header row is fixed
- body scrolls independently
- tapping a row changes selected point for manual operations below

### Main Header

- profile name shown in available width
- settings action uses a fixed-size gear icon
- title/header actions are intended to remain visually stable even for long profile names

### Value Panel

- when auto simulation is running, panel color changes to indicate auto mode
- color choice is intentionally subdued rather than bright or purple-biased

## Settings Screen

### Header Actions

Contains:
- language icon button in the header
- tap opens a dropdown menu of supported app languages
- current selection is indicated inside the dropdown
- back action uses the shared fixed-size icon button

### Language Switching

Current supported app languages:
- English
- Japanese

Behavior:
- selection is persisted locally
- stored language is applied again on next startup
- switching language updates resource-based UI strings

### Sections Order

Current order:
1. Meter Preset
2. USB-RS485
3. Logs
4. Comm Test
5. App Update

### Meter Preset Section

Contains:
- preset selector
- user-added preset indicator (`added` tag)
- `Edit Meter`
- slave address input and apply button
- main view mode toggle button

### USB-RS485 Section

Contains:
- detected USB device counts
- current profile serial settings
- connection status
- refresh button
- per-device permission/connect/disconnect actions

### Logs Section

Contains:
- log count
- open logs action

### Comm Test Section

Contains:
- current selected point display
- quick read test button
- custom HEX request input
- custom request send button

### App Update Section

Located at the bottom of settings screen.

Contains:
- current app version (`X.Y.Z` only)
- update status message
- latest version if known
- download progress if active
- single action button for check/download flow

## Shared Header Rules

- back action uses a fixed-size arrow icon, not text
- edit/delete/settings/language utility actions prefer fixed-size icons for layout stability
- screen headers are kept outside the scrolling content region where appropriate

## Edit Meter Screen

- shows user-added meters only by default
- can reveal built-in preset list through `Show Presets`
- built-in presets are view-only
- user-added meters are editable
- delete mode allows multi-select deletion

## Add Meter / Register Settings Screen

- standard 22 template signal names are locked and not freely editable
- unused template entries are skipped by leaving address blank
- `Add Register` allows custom non-template registers
- validation errors appear before overwrite confirmation
- standard fields include communication settings and register definitions

## Logs Screen

- supports icon-based export and clear actions
- shows categorized logs
- can share exported log file via Android share sheet

## Log Summary Screen

- separate view from raw logs
- groups USB RX requests by:
  - slave ID
  - function code
  - start address
  - quantity
- useful for understanding what external masters actually read

## Related Code

- `app/src/main/java/com/example/meterdemo/ui/*`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/meterdemo/localization/*`

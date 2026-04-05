# Simulation Specification

## Scope

Document value simulation behavior and assumptions.

## General Principles

- Simulation is optional and controlled from Main screen.
- Manual input and automatic simulation share the same underlying register repository.
- Simulation uses floating-point internal state for smoother motion than displayed rounded values.
- Display rounding happens at UI formatting time, not inside the simulation state itself.
- Derived relationships should remain physically consistent where possible.

## Tick Timing

- Simulation tick loop interval target:
  - `1000 ms`
- Effective integration uses measured elapsed real time between ticks, not a fixed assumed second.
- Elapsed seconds are clamped to avoid runaway integration after long pauses.

## Reset / Seed Rules

Simulation state is reset from current displayed values when:
- the simulation is started
- the selected preset/profile changes
- values are manually edited and applied
- values are reset

This means:
- manual input becomes the new internal starting point
- auto mode continues from the new seeded state

## Voltage Behavior

Target signal types:
- phase voltages
- line voltages

Behavior:
- random walk around initial displayed value
- allowed range:
  - initial value �}10%
- each tick adds a small delta within that range
- value is clamped to min/max range

## Current Behavior

Target signal types:
- A���d��
- B���d��
- C���d��

Behavior uses a shared signed base current for all three phases.

### Shared Base Current

State held:
- `baseValue`
- `targetBaseValue`
- `ticksUntilNextEvent`
- `transitionTicksRemaining`
- sign is part of `baseValue`

### Normal Operation

- `baseValue` stays fixed between major events
- each phase varies independently within:
  - `baseValue �}5%`
- this keeps phases close together while still giving small differences

### Major Events

- major event interval:
  - random `60..200` ticks
- when an event starts:
  - a new signed `targetBaseValue` is chosen
  - magnitude is based on current baseline scale
  - sign may occasionally flip by crossing zero
- transition duration:
  - random `1..10` ticks
- during transition:
  - `baseValue` moves toward `targetBaseValue`
  - each phase still varies within `baseValue �}5%`
- after transition completes:
  - next event interval is generated again from `60..200`

## Power Factor Behavior

Target signal type:
- �͗�

Behavior:
- stable at base value most of the time
- occasional burst drop events
- next burst interval:
  - random `15..35` ticks
- burst duration:
  - random `3..7` ticks
- burst value:
  - base value scaled down to about `45%..85%`
- clamped to `-0.999..0.999`

## Derived Power Calculations

The simulation does not directly animate active power. It is derived.

### Voltage Selection

- If phase voltages exist, use them directly per phase.
- If only line voltages exist, infer phase-equivalent voltage from:
  - `average line voltage / sqrt(3)`

### Apparent Power

Per phase:
- `S_phase = V_phase x |I_phase| / 1000`

Total:
- `S_total = sum(S_phase)`

### Active Power

Per phase:
- `P_phase = S_phase x PF x sign(I_phase)`

Total:
- `P_total = sum(P_phase)`

### Reactive Power

Per phase magnitude:
- `Q_phase_mag = S_phase x sqrt(1 - PF^2)`

Per phase sign:
- sign follows `P_phase`

Total:
- `Q_total = sum(Q_phase)`

## Energy Integration

Energy values are derived from power and elapsed time.

### Active Energy

- `delta_kWh = P_total x elapsedHours`
- if `P_total >= 0`:
  - add to forward active energy
- if `P_total < 0`:
  - add to reverse active energy
- total active energy:
  - if forward/reverse totals exist, total becomes `forward + reverse`
  - otherwise accumulate absolute active energy

### Reactive Energy

- `delta_kVarh = |Q_total| x elapsedHours`
- total reactive energy accumulates absolute reactive energy
- forward/reverse reactive totals are currently held but not independently derived beyond preserving existing values

## Display and Raw Conversion

- Simulation produces floating-point display-domain values
- `MainViewModel` converts display values back to raw register values using the point's data type and gain
- INT values are rounded before writing raw values back to repository
- FLOAT values are stored as 32-bit float bits

## Expected Result

The intended visual effect is:
- voltages drift slowly
- phases stay reasonably coherent
- currents show small normal movement plus occasional larger load changes
- active power changes naturally as a consequence of V, I, and PF
- energy totals integrate smoothly over time

## Related Code

- `app/src/main/java/com/example/meterdemo/meter/simulation/MeterSimulationEngine.kt`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`

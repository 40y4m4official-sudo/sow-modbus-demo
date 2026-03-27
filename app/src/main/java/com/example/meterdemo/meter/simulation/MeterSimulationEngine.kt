package com.example.meterdemo.meter.simulation

import com.example.meterdemo.meter.model.MeterPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.random.Random

class MeterSimulationEngine(
    private val random: Random = Random.Default
) {
    private val voltageStates = mutableMapOf<Int, VoltageState>()
    private val currentStates = mutableMapOf<Int, CurrentState>()
    private val powerFactorStates = mutableMapOf<Int, PowerFactorState>()
    private val energyStates = mutableMapOf<Int, Double>()

    fun reset(points: List<MeterPoint>, displayValues: Map<Int, Double>) {
        voltageStates.clear()
        currentStates.clear()
        powerFactorStates.clear()
        energyStates.clear()

        points.forEach { point ->
            val displayValue = displayValues[point.address] ?: point.displayValue(point.initialRawValue)

            when {
                point.isVoltagePoint() -> {
                    val span = max(abs(displayValue) * 0.10, 1.0)
                    voltageStates[point.address] = VoltageState(
                        currentValue = displayValue,
                        minimum = displayValue - span,
                        maximum = displayValue + span
                    )
                }

                point.isCurrentPoint() -> {
                    currentStates[point.address] = CurrentState(
                        currentValue = displayValue,
                        ticksUntilNextStep = nextStepInterval()
                    )
                }

                point.isPowerFactorPoint() -> {
                    powerFactorStates[point.address] = PowerFactorState(
                        baseValue = displayValue.coerceIn(-0.999, 0.999),
                        currentValue = displayValue.coerceIn(-0.999, 0.999),
                        burstTicksRemaining = 0,
                        ticksUntilNextBurst = nextBurstInterval()
                    )
                }

                point.isEnergyPoint() -> {
                    energyStates[point.address] = max(0.0, displayValue)
                }
            }
        }
    }

    fun tick(points: List<MeterPoint>, elapsedSeconds: Double): Map<Int, Double> {
        val updatedValues = mutableMapOf<Int, Double>()

        points.filter { it.isVoltagePoint() }.forEach { point ->
            val state = voltageStates.getOrPut(point.address) {
                val initial = point.displayValue(point.initialRawValue)
                val span = max(abs(initial) * 0.10, 1.0)
                VoltageState(initial, initial - span, initial + span)
            }

            val span = max(state.maximum - state.minimum, 0.1)
            val delta = span * random.nextDouble(-0.03, 0.03)
            state.currentValue = (state.currentValue + delta).coerceIn(state.minimum, state.maximum)
            updatedValues[point.address] = state.currentValue
        }

        points.filter { it.isCurrentPoint() }.forEach { point ->
            val state = currentStates.getOrPut(point.address) {
                CurrentState(point.displayValue(point.initialRawValue), nextStepInterval())
            }

            if (state.ticksUntilNextStep <= 0) {
                val baseMagnitude = max(abs(point.displayValue(point.initialRawValue)), 1.0)
                val stepMagnitude = baseMagnitude * random.nextDouble(0.2, 1.8)
                state.currentValue = stepMagnitude * if (random.nextBoolean()) 1.0 else -1.0
                state.ticksUntilNextStep = nextStepInterval()
            } else {
                state.ticksUntilNextStep -= 1
            }

            updatedValues[point.address] = state.currentValue
        }

        points.filter { it.isPowerFactorPoint() }.forEach { point ->
            val state = powerFactorStates.getOrPut(point.address) {
                val initial = point.displayValue(point.initialRawValue).coerceIn(-0.999, 0.999)
                PowerFactorState(initial, initial, 0, nextBurstInterval())
            }

            if (state.burstTicksRemaining > 0) {
                state.burstTicksRemaining -= 1
                if (state.burstTicksRemaining == 0) {
                    state.currentValue = state.baseValue
                    state.ticksUntilNextBurst = nextBurstInterval()
                }
            } else {
                state.ticksUntilNextBurst -= 1
                if (state.ticksUntilNextBurst <= 0) {
                    val burstScale = random.nextDouble(0.45, 0.85)
                    state.currentValue = (state.baseValue * burstScale).coerceIn(-0.999, 0.999)
                    state.burstTicksRemaining = random.nextInt(3, 8)
                }
            }

            updatedValues[point.address] = state.currentValue
        }

        val phaseVoltages = points
            .filter { it.isPhaseVoltagePoint() }
            .associate { point -> phaseKey(point.name) to (updatedValues[point.address] ?: point.displayValue(point.initialRawValue)) }

        val lineVoltages = points
            .filter { it.isLineVoltagePoint() }
            .associate { point -> point.address to (updatedValues[point.address] ?: point.displayValue(point.initialRawValue)) }

        val phaseCurrents = points
            .filter { it.isCurrentPoint() }
            .associate { point -> phaseKey(point.name) to (updatedValues[point.address] ?: point.displayValue(point.initialRawValue)) }

        val totalPowerFactor = points
            .firstOrNull { it.isPowerFactorPoint() }
            ?.let { updatedValues[it.address] ?: it.displayValue(it.initialRawValue) }
            ?.coerceIn(-0.999, 0.999)
            ?: 0.95

        val averageVoltage = when {
            phaseVoltages.isNotEmpty() -> phaseVoltages.values.average()
            lineVoltages.isNotEmpty() -> lineVoltages.values.average()
            else -> 0.0
        }
        val signedTotalCurrent = phaseCurrents.values.sum()
        val totalCurrentMagnitude = phaseCurrents.values.sumOf { abs(it) }
        val apparentPower = (averageVoltage * totalCurrentMagnitude) / 1000.0
        val activePower = apparentPower * totalPowerFactor * if (signedTotalCurrent == 0.0) 0.0 else sign(signedTotalCurrent)
        val reactivePowerMagnitude = apparentPower * sqrt(max(0.0, 1.0 - (totalPowerFactor * totalPowerFactor)))
        val reactivePower = reactivePowerMagnitude * if (activePower == 0.0) 0.0 else sign(activePower)

        val totalCurrentForShare = if (totalCurrentMagnitude <= 0.0001) 1.0 else totalCurrentMagnitude
        val phaseShares = phaseCurrents.mapValues { (_, current) -> abs(current) / totalCurrentForShare }

        points.filter { it.isPowerPoint() }.forEach { point ->
            val value = when {
                point.isActivePowerPoint() && point.isPhasePoint() -> {
                    val share = phaseShares[phaseKey(point.name)] ?: 0.0
                    activePower * share
                }

                point.isActivePowerPoint() -> activePower
                point.isReactivePowerPoint() && point.isPhasePoint() -> {
                    val share = phaseShares[phaseKey(point.name)] ?: 0.0
                    reactivePower * share
                }

                point.isReactivePowerPoint() -> reactivePower
                point.isApparentPowerPoint() && point.isPhasePoint() -> {
                    val share = phaseShares[phaseKey(point.name)] ?: 0.0
                    apparentPower * share
                }

                point.isApparentPowerPoint() -> apparentPower
                else -> point.displayValue(point.initialRawValue)
            }

            updatedValues[point.address] = value
        }

        val hoursPerTick = (elapsedSeconds.coerceAtLeast(0.0)) / 3600.0
        val activeEnergyDelta = activePower * hoursPerTick
        val reactiveEnergyDelta = abs(reactivePower) * hoursPerTick

        val activeEnergyPoints = points.filter { it.unit == "kWh" }.sortedBy { it.address }
        val reactiveEnergyPoints = points.filter { it.unit == "kVarh" }.sortedBy { it.address }

        val activeAssignments = assignActiveEnergyRoles(activeEnergyPoints)
        val reactiveAssignments = assignReactiveEnergyRoles(reactiveEnergyPoints)

        activeAssignments.forward?.let { point ->
            val base = energyStates.getOrPut(point.address) { point.displayValue(point.initialRawValue) }
            val nextValue = if (activeEnergyDelta >= 0.0) base + activeEnergyDelta else base
            energyStates[point.address] = nextValue
            updatedValues[point.address] = nextValue
        }

        activeAssignments.reverse?.let { point ->
            val base = energyStates.getOrPut(point.address) { point.displayValue(point.initialRawValue) }
            val nextValue = if (activeEnergyDelta < 0.0) base + abs(activeEnergyDelta) else base
            energyStates[point.address] = nextValue
            updatedValues[point.address] = nextValue
        }

        activeAssignments.total?.let { point ->
            val base = energyStates.getOrPut(point.address) { point.displayValue(point.initialRawValue) }
            val forwardValue = activeAssignments.forward?.let { energyStates.getOrDefault(it.address, it.displayValue(it.initialRawValue)) } ?: 0.0
            val reverseValue = activeAssignments.reverse?.let { energyStates.getOrDefault(it.address, it.displayValue(it.initialRawValue)) } ?: 0.0
            val nextValue = if (activeAssignments.forward != null || activeAssignments.reverse != null) {
                max(base, forwardValue + reverseValue)
            } else {
                base + abs(activeEnergyDelta)
            }
            energyStates[point.address] = nextValue
            updatedValues[point.address] = nextValue
        }

        reactiveAssignments.total?.let { point ->
            val base = energyStates.getOrPut(point.address) { point.displayValue(point.initialRawValue) }
            val nextValue = base + reactiveEnergyDelta
            energyStates[point.address] = nextValue
            updatedValues[point.address] = nextValue
        }

        reactiveAssignments.forward?.let { point ->
            val base = energyStates.getOrPut(point.address) { point.displayValue(point.initialRawValue) }
            energyStates[point.address] = base
            updatedValues[point.address] = base
        }

        reactiveAssignments.reverse?.let { point ->
            val base = energyStates.getOrPut(point.address) { point.displayValue(point.initialRawValue) }
            energyStates[point.address] = base
            updatedValues[point.address] = base
        }

        return updatedValues
    }

    private fun assignActiveEnergyRoles(points: List<MeterPoint>): EnergyAssignments {
        val forward = points.firstOrNull { it.isForwardEnergyPoint() }
        val reverse = points.firstOrNull { it.isReverseEnergyPoint() }
        val total = points.firstOrNull { it.isTotalEnergyPoint() }

        if (forward != null || reverse != null || total != null) {
            return EnergyAssignments(total = total, forward = forward, reverse = reverse)
        }

        return when (points.size) {
            0 -> EnergyAssignments()
            1 -> EnergyAssignments(total = points[0])
            2 -> EnergyAssignments(forward = points[0], reverse = points[1])
            else -> EnergyAssignments(total = points[0], forward = points[1], reverse = points[2])
        }
    }

    private fun assignReactiveEnergyRoles(points: List<MeterPoint>): EnergyAssignments {
        val forward = points.firstOrNull { it.isForwardEnergyPoint() }
        val reverse = points.firstOrNull { it.isReverseEnergyPoint() }
        val total = points.firstOrNull { it.isTotalEnergyPoint() }

        if (forward != null || reverse != null || total != null) {
            return EnergyAssignments(total = total, forward = forward, reverse = reverse)
        }

        return when (points.size) {
            0 -> EnergyAssignments()
            1 -> EnergyAssignments(total = points[0])
            2 -> EnergyAssignments(forward = points[0], reverse = points[1])
            else -> EnergyAssignments(total = points[0], forward = points[1], reverse = points[2])
        }
    }

    private fun nextStepInterval(): Int = random.nextInt(3, 8)

    private fun nextBurstInterval(): Int = random.nextInt(15, 36)

    private fun MeterPoint.isVoltagePoint(): Boolean = unit == "V"

    private fun MeterPoint.isCurrentPoint(): Boolean = unit == "A"

    private fun MeterPoint.isPowerFactorPoint(): Boolean {
        return name.contains("Power Factor", ignoreCase = true) || name.contains("蜉帷紫")
    }

    private fun MeterPoint.isPowerPoint(): Boolean = unit == "kW" || unit == "kVar" || unit == "kVA"

    private fun MeterPoint.isActivePowerPoint(): Boolean = unit == "kW"

    private fun MeterPoint.isReactivePowerPoint(): Boolean = unit == "kVar"

    private fun MeterPoint.isApparentPowerPoint(): Boolean = unit == "kVA"

    private fun MeterPoint.isEnergyPoint(): Boolean = unit == "kWh" || unit == "kVarh"

    private fun MeterPoint.isLineVoltagePoint(): Boolean {
        val upperName = name.uppercase()
        return unit == "V" && (upperName.contains("A-B") || upperName.contains("B-C") || upperName.contains("C-A"))
    }

    private fun MeterPoint.isPhaseVoltagePoint(): Boolean = isVoltagePoint() && !isLineVoltagePoint()

    private fun MeterPoint.isPhasePoint(): Boolean {
        val upperName = name.uppercase()
        return upperName.startsWith("A ") ||
            upperName.startsWith("B ") ||
            upperName.startsWith("C ") ||
            (upperName.startsWith("A") && !upperName.startsWith("A-") && !upperName.startsWith("ACTIVE") && !upperName.startsWith("APPARENT")) ||
            upperName.startsWith("B-").not() && upperName.startsWith("B") ||
            upperName.startsWith("C-").not() && upperName.startsWith("C")
    }

    private fun MeterPoint.isForwardEnergyPoint(): Boolean {
        val upperName = name.uppercase()
        return upperName.contains("FORWARD") || upperName.contains("IMPORT")
    }

    private fun MeterPoint.isReverseEnergyPoint(): Boolean {
        val upperName = name.uppercase()
        return upperName.contains("REVERSE") || upperName.contains("EXPORT")
    }

    private fun MeterPoint.isTotalEnergyPoint(): Boolean {
        val upperName = name.uppercase()
        return upperName.contains("TOTAL") && !isForwardEnergyPoint() && !isReverseEnergyPoint()
    }

    private fun phaseKey(name: String): String {
        val upperName = name.uppercase()
        return when {
            (upperName.startsWith("A") && !upperName.startsWith("A-") && !upperName.startsWith("ACTIVE") && !upperName.startsWith("APPARENT")) || upperName.contains("PHASE A") -> "A"
            upperName.startsWith("B") || upperName.contains("PHASE B") -> "B"
            upperName.startsWith("C") || upperName.contains("PHASE C") -> "C"
            else -> ""
        }
    }
}

private data class VoltageState(
    var currentValue: Double,
    val minimum: Double,
    val maximum: Double
)

private data class CurrentState(
    var currentValue: Double,
    var ticksUntilNextStep: Int
)

private data class PowerFactorState(
    val baseValue: Double,
    var currentValue: Double,
    var burstTicksRemaining: Int,
    var ticksUntilNextBurst: Int
)

private data class EnergyAssignments(
    val total: MeterPoint? = null,
    val forward: MeterPoint? = null,
    val reverse: MeterPoint? = null
)

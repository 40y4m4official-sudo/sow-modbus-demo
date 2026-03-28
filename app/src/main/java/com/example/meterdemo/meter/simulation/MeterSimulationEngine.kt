package com.example.meterdemo.meter.simulation

import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.SignalType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.random.Random

class MeterSimulationEngine(
    private val random: Random = Random.Default
) {
    private companion object {
        private const val SQRT_3 = 1.7320508075688772
    }

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
                point.signalType.isVoltageType() -> {
                    val span = max(abs(displayValue) * 0.10, 1.0)
                    voltageStates[point.address] = VoltageState(
                        currentValue = displayValue,
                        minimum = displayValue - span,
                        maximum = displayValue + span
                    )
                }

                point.signalType.isCurrentType() -> {
                    currentStates[point.address] = CurrentState(
                        currentValue = displayValue,
                        targetValue = displayValue,
                        ticksUntilNextStep = nextStepInterval()
                    )
                }

                point.signalType == SignalType.POWER_FACTOR -> {
                    powerFactorStates[point.address] = PowerFactorState(
                        baseValue = displayValue.coerceIn(-0.999, 0.999),
                        currentValue = displayValue.coerceIn(-0.999, 0.999),
                        burstTicksRemaining = 0,
                        ticksUntilNextBurst = nextBurstInterval()
                    )
                }

                point.signalType.isEnergyType() -> {
                    energyStates[point.address] = max(0.0, displayValue)
                }
            }
        }
    }

    fun tick(points: List<MeterPoint>, elapsedSeconds: Double): Map<Int, Double> {
        val updatedValues = mutableMapOf<Int, Double>()

        points.filter { it.signalType.isVoltageType() }.forEach { point ->
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

        points.filter { it.signalType.isCurrentType() }.forEach { point ->
            val state = currentStates.getOrPut(point.address) {
                val initial = point.displayValue(point.initialRawValue)
                CurrentState(
                    currentValue = initial,
                    targetValue = initial,
                    ticksUntilNextStep = nextStepInterval()
                )
            }

            if (state.ticksUntilNextStep <= 0) {
                val baseMagnitude = max(abs(point.displayValue(point.initialRawValue)), 1.0)
                val currentSign = when {
                    state.currentValue > 0.0 -> 1.0
                    state.currentValue < 0.0 -> -1.0
                    else -> if (random.nextBoolean()) 1.0 else -1.0
                }
                val shouldCrossZero = random.nextDouble() < 0.28
                val nextSign = if (shouldCrossZero) -currentSign else currentSign
                val stepMagnitude = baseMagnitude * random.nextDouble(0.2, 1.8)
                state.targetValue = stepMagnitude * nextSign
                state.ticksUntilNextStep = nextStepInterval()
            } else {
                state.ticksUntilNextStep -= 1
            }

            val delta = state.targetValue - state.currentValue
            val smoothing = if (delta == 0.0) 0.0 else max(abs(delta) * 0.45, 0.05)
            if (abs(delta) <= smoothing) {
                state.currentValue = state.targetValue
            } else {
                state.currentValue += smoothing * kotlin.math.sign(delta)
            }

            updatedValues[point.address] = state.currentValue
        }

        points.filter { it.signalType == SignalType.POWER_FACTOR }.forEach { point ->
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
            .filter { it.signalType.isPhaseVoltageType() }
            .associate { point -> phaseKey(point.signalType) to (updatedValues[point.address] ?: point.displayValue(point.initialRawValue)) }

        val lineVoltages = points
            .filter { it.signalType.isLineVoltageType() }
            .associate { point -> point.address to (updatedValues[point.address] ?: point.displayValue(point.initialRawValue)) }

        val phaseCurrents = points
            .filter { it.signalType.isCurrentType() }
            .associate { point -> phaseKey(point.signalType) to (updatedValues[point.address] ?: point.displayValue(point.initialRawValue)) }

        val totalPowerFactor = points
            .firstOrNull { it.signalType == SignalType.POWER_FACTOR }
            ?.let { updatedValues[it.address] ?: it.displayValue(it.initialRawValue) }
            ?.coerceIn(-0.999, 0.999)
            ?: 0.95

        val averageVoltage = when {
            phaseVoltages.isNotEmpty() -> phaseVoltages.values.average()
            lineVoltages.isNotEmpty() -> lineVoltages.values.average()
            else -> 0.0
        }
        val averagePhaseEquivalentVoltage = when {
            phaseVoltages.isNotEmpty() -> averageVoltage
            lineVoltages.isNotEmpty() -> averageVoltage / SQRT_3
            else -> 0.0
        }
        val inferredPhaseVoltages = when {
            phaseVoltages.isNotEmpty() -> phaseVoltages
            phaseCurrents.isNotEmpty() -> listOf("A", "B", "C").associateWith { averagePhaseEquivalentVoltage }
            else -> emptyMap()
        }
        val phaseApparentPowers = listOf("A", "B", "C").associateWith { phase ->
            val phaseVoltage = inferredPhaseVoltages[phase] ?: averagePhaseEquivalentVoltage
            val phaseCurrent = phaseCurrents[phase] ?: 0.0
            (phaseVoltage * abs(phaseCurrent)) / 1000.0
        }
        val phaseActivePowers = listOf("A", "B", "C").associateWith { phase ->
            val current = phaseCurrents[phase] ?: 0.0
            val signedFactor = if (current == 0.0) 0.0 else sign(current)
            phaseApparentPowers.getValue(phase) * totalPowerFactor * signedFactor
        }
        val apparentPower = phaseApparentPowers.values.sum()
        val activePower = phaseActivePowers.values.sum()
        val phaseReactivePowers = listOf("A", "B", "C").associateWith { phase ->
            val signedFactor = phaseActivePowers.getValue(phase).let { if (it == 0.0) 0.0 else sign(it) }
            val reactiveMagnitude = phaseApparentPowers.getValue(phase) * sqrt(max(0.0, 1.0 - (totalPowerFactor * totalPowerFactor)))
            reactiveMagnitude * signedFactor
        }
        val reactivePower = phaseReactivePowers.values.sum()

        points.filter { it.signalType.isPowerType() }.forEach { point ->
            val value = when {
                point.signalType.isActivePowerType() && point.signalType.isPhasePowerType() ->
                    phaseActivePowers[phaseKey(point.signalType)] ?: 0.0
                point.signalType.isActivePowerType() -> activePower
                point.signalType.isReactivePowerType() && point.signalType.isPhasePowerType() ->
                    phaseReactivePowers[phaseKey(point.signalType)] ?: 0.0
                point.signalType.isReactivePowerType() -> reactivePower
                point.signalType.isApparentPowerType() && point.signalType.isPhasePowerType() ->
                    phaseApparentPowers[phaseKey(point.signalType)] ?: 0.0
                point.signalType.isApparentPowerType() -> apparentPower
                else -> point.displayValue(point.initialRawValue)
            }

            updatedValues[point.address] = value
        }

        val hoursPerTick = (elapsedSeconds.coerceAtLeast(0.0)) / 3600.0
        val activeEnergyDelta = activePower * hoursPerTick
        val reactiveEnergyDelta = abs(reactivePower) * hoursPerTick

        val activeEnergyPoints = points.filter { it.signalType.isActiveEnergyType() }.sortedBy { it.address }
        val reactiveEnergyPoints = points.filter { it.signalType.isReactiveEnergyType() }.sortedBy { it.address }

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

    private fun MeterPoint.isForwardEnergyPoint(): Boolean {
        return signalType == SignalType.FORWARD_ACTIVE_ENERGY_TOTAL ||
            signalType == SignalType.FORWARD_REACTIVE_ENERGY_TOTAL
    }

    private fun MeterPoint.isReverseEnergyPoint(): Boolean {
        return signalType == SignalType.REVERSE_ACTIVE_ENERGY_TOTAL ||
            signalType == SignalType.REVERSE_REACTIVE_ENERGY_TOTAL
    }

    private fun MeterPoint.isTotalEnergyPoint(): Boolean {
        return signalType == SignalType.TOTAL_ACTIVE_ENERGY ||
            signalType == SignalType.TOTAL_REACTIVE_ENERGY
    }

    private fun phaseKey(signalType: SignalType): String {
        return when (signalType) {
            SignalType.PHASE_A_VOLTAGE,
            SignalType.PHASE_A_CURRENT,
            SignalType.PHASE_A_ACTIVE_POWER -> "A"
            SignalType.PHASE_B_VOLTAGE,
            SignalType.PHASE_B_CURRENT,
            SignalType.PHASE_B_ACTIVE_POWER -> "B"
            SignalType.PHASE_C_VOLTAGE,
            SignalType.PHASE_C_CURRENT,
            SignalType.PHASE_C_ACTIVE_POWER -> "C"
            else -> ""
        }
    }
}

private fun SignalType.isVoltageType(): Boolean = isPhaseVoltageType() || isLineVoltageType()

private fun SignalType.isPhaseVoltageType(): Boolean = this in setOf(
    SignalType.PHASE_A_VOLTAGE,
    SignalType.PHASE_B_VOLTAGE,
    SignalType.PHASE_C_VOLTAGE
)

private fun SignalType.isLineVoltageType(): Boolean = this in setOf(
    SignalType.LINE_VOLTAGE_AB,
    SignalType.LINE_VOLTAGE_BC,
    SignalType.LINE_VOLTAGE_CA
)

private fun SignalType.isCurrentType(): Boolean = this in setOf(
    SignalType.PHASE_A_CURRENT,
    SignalType.PHASE_B_CURRENT,
    SignalType.PHASE_C_CURRENT
)

private fun SignalType.isPowerType(): Boolean = isActivePowerType() || isReactivePowerType() || isApparentPowerType()

private fun SignalType.isActivePowerType(): Boolean = this in setOf(
    SignalType.ACTIVE_POWER_TOTAL,
    SignalType.PHASE_A_ACTIVE_POWER,
    SignalType.PHASE_B_ACTIVE_POWER,
    SignalType.PHASE_C_ACTIVE_POWER
)

private fun SignalType.isReactivePowerType(): Boolean = this == SignalType.REACTIVE_POWER_TOTAL

private fun SignalType.isApparentPowerType(): Boolean = this == SignalType.APPARENT_POWER_TOTAL

private fun SignalType.isPhasePowerType(): Boolean = this in setOf(
    SignalType.PHASE_A_ACTIVE_POWER,
    SignalType.PHASE_B_ACTIVE_POWER,
    SignalType.PHASE_C_ACTIVE_POWER
)

private fun SignalType.isEnergyType(): Boolean = isActiveEnergyType() || isReactiveEnergyType()

private fun SignalType.isActiveEnergyType(): Boolean = this in setOf(
    SignalType.TOTAL_ACTIVE_ENERGY,
    SignalType.FORWARD_ACTIVE_ENERGY_TOTAL,
    SignalType.REVERSE_ACTIVE_ENERGY_TOTAL
)

private fun SignalType.isReactiveEnergyType(): Boolean = this in setOf(
    SignalType.TOTAL_REACTIVE_ENERGY,
    SignalType.FORWARD_REACTIVE_ENERGY_TOTAL,
    SignalType.REVERSE_REACTIVE_ENERGY_TOTAL
)

private data class VoltageState(
    var currentValue: Double,
    val minimum: Double,
    val maximum: Double
)

private data class CurrentState(
    var currentValue: Double,
    var targetValue: Double,
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

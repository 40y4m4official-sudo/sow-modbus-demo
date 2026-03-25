package com.example.meterdemo.viewmodel

import androidx.lifecycle.ViewModel
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.CommLogger
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.profiles.MeterProfiles
import com.example.meterdemo.meter.repository.MeterRepository
import com.example.meterdemo.meter.repository.MeterValueSnapshot
import com.example.meterdemo.modbus.ModbusCrc
import com.example.meterdemo.modbus.ModbusFrameParser
import com.example.meterdemo.modbus.ModbusRtuSlaveEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val repository = MeterRepository(MeterProfiles.default())
    private val engine = ModbusRtuSlaveEngine(repository)
    private val logger = CommLogger()

    private val _uiState = MutableStateFlow(createUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val logs: StateFlow<List<CommLog>> = logger.logs

    fun nextPoint() {
        val snapshots = repository.snapshot()
        val currentIndex = _uiState.value.selectedPointIndex
        val nextIndex = if (snapshots.isEmpty()) 0 else (currentIndex + 1) % snapshots.size
        refreshUiState(selectedPointIndex = nextIndex)
    }

    fun previousPoint() {
        val snapshots = repository.snapshot()
        val currentIndex = _uiState.value.selectedPointIndex
        val previousIndex = if (snapshots.isEmpty()) {
            0
        } else {
            (currentIndex - 1 + snapshots.size) % snapshots.size
        }
        refreshUiState(selectedPointIndex = previousIndex)
    }

    fun updateSelectedRawValue(text: String) {
        refreshUiState(rawValueInput = text)
    }

    fun applySelectedRawValue() {
        val state = _uiState.value
        val point = state.selectedPoint ?: return
        val value = state.rawValueInput.toIntOrNull()

        if (value == null) {
            logger.error("Raw value must be numeric")
            return
        }

        if (!repository.setRawValue(point.address, value)) {
            logger.error("Unsupported address: ${point.address}")
            return
        }

        logger.info("Updated raw value: address=${point.address}, value=$value")
        refreshUiState(selectedPointIndex = state.selectedPointIndex)
    }

    fun resetValues() {
        repository.resetCurrentProfileValues()
        logger.info("Reset all register values")
        refreshUiState(selectedPointIndex = _uiState.value.selectedPointIndex)
    }

    fun updateSlaveIdInput(text: String) {
        refreshUiState(slaveIdInput = text)
    }

    fun applySlaveId() {
        val state = _uiState.value
        val slaveId = state.slaveIdInput.toIntOrNull()
        if (slaveId == null) {
            logger.error("Slave ID must be numeric")
            return
        }
        if (!repository.setSlaveId(slaveId)) {
            logger.error("Slave ID must be in range 1..247")
            return
        }
        logger.info("Updated slave ID: $slaveId")
        refreshUiState(selectedPointIndex = state.selectedPointIndex)
    }

    fun selectProfile(modelId: String) {
        val profile = MeterProfiles.findByModelId(modelId)
        if (profile == null) {
            logger.error("Preset not found: $modelId")
            return
        }

        repository.loadProfile(profile)
        logger.info("Switched preset: ${profile.displayName}")
        refreshUiState(selectedPointIndex = 0)
    }

    fun refreshLogs() {
        logger.info("Refreshed logs")
        refreshUiState(selectedPointIndex = _uiState.value.selectedPointIndex)
    }

    fun clearLogs() {
        logger.clear()
        logger.info("Cleared logs")
        refreshUiState(selectedPointIndex = _uiState.value.selectedPointIndex)
    }

    fun simulateReadOfSelectedPoint() {
        val point = _uiState.value.selectedPoint ?: return
        simulateRead(point.address)
    }

    fun simulateCustomRequest(hexText: String) {
        val frame = parseHexString(hexText)
        if (frame == null) {
            logger.error("Failed to parse HEX request")
            return
        }

        logger.rx(ModbusFrameParser.toHexString(frame), "Custom request")

        val response = engine.handleRequest(frame)
        if (response == null) {
            logger.error("Failed to create response")
            return
        }

        logger.tx(ModbusFrameParser.toHexString(response), "Custom response")
        refreshUiState(selectedPointIndex = _uiState.value.selectedPointIndex)
    }

    private fun simulateRead(address: Int) {
        val requestWithoutCrc = byteArrayOf(
            repository.getSlaveId().toByte(),
            repository.getFunctionCode().toByte(),
            ((address shr 8) and 0xFF).toByte(),
            (address and 0xFF).toByte(),
            0x00,
            0x01
        )

        val request = ModbusCrc.appendCrc(requestWithoutCrc)
        logger.rx(ModbusFrameParser.toHexString(request), "Read request addr=$address")

        val response = engine.handleRequest(request)
        if (response == null) {
            logger.error("Failed to create response")
            return
        }

        logger.tx(ModbusFrameParser.toHexString(response), "Read response")
        refreshUiState(selectedPointIndex = _uiState.value.selectedPointIndex)
    }

    private fun refreshUiState(
        selectedPointIndex: Int = _uiState.value.selectedPointIndex,
        rawValueInput: String? = null,
        slaveIdInput: String? = null
    ) {
        val snapshots = repository.snapshot()
        val safeIndex = when {
            snapshots.isEmpty() -> 0
            selectedPointIndex < 0 -> 0
            selectedPointIndex > snapshots.lastIndex -> snapshots.lastIndex
            else -> selectedPointIndex
        }
        val selectedPoint = snapshots.getOrNull(safeIndex)

        _uiState.value = MainUiState(
            profileName = repository.getProfile().displayName,
            profileModelId = repository.getProfile().modelId,
            profiles = MeterProfiles.all(),
            slaveId = repository.getSlaveId(),
            slaveIdInput = slaveIdInput ?: repository.getSlaveId().toString(),
            points = snapshots,
            selectedPointIndex = safeIndex,
            selectedPoint = selectedPoint,
            rawValueInput = rawValueInput ?: selectedPoint?.rawValue?.toString().orEmpty()
        )
    }

    private fun createUiState(): MainUiState {
        val snapshots = repository.snapshot()
        val selectedPoint = snapshots.firstOrNull()
        return MainUiState(
            profileName = repository.getProfile().displayName,
            profileModelId = repository.getProfile().modelId,
            profiles = MeterProfiles.all(),
            slaveId = repository.getSlaveId(),
            slaveIdInput = repository.getSlaveId().toString(),
            points = snapshots,
            selectedPointIndex = 0,
            selectedPoint = selectedPoint,
            rawValueInput = selectedPoint?.rawValue?.toString().orEmpty()
        )
    }

    private fun parseHexString(input: String): ByteArray? {
        val cleaned = input
            .replace("\n", " ")
            .replace("\t", " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }

        return try {
            ByteArray(cleaned.size) { index ->
                cleaned[index].toInt(16).toByte()
            }
        } catch (_: Exception) {
            null
        }
    }
}

data class MainUiState(
    val profileName: String,
    val profileModelId: String,
    val profiles: List<MeterProfile>,
    val slaveId: Int,
    val slaveIdInput: String,
    val points: List<MeterValueSnapshot>,
    val selectedPointIndex: Int,
    val selectedPoint: MeterValueSnapshot?,
    val rawValueInput: String
)

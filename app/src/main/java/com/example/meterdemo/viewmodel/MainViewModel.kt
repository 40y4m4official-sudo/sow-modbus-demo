package com.example.meterdemo.viewmodel

import androidx.lifecycle.ViewModel
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.CommLogger
import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
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

    private val builtinProfiles = MeterProfiles.all()
    private val userProfiles = mutableListOf<MeterProfile>()
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
        val previousIndex = if (snapshots.isEmpty()) 0 else (currentIndex - 1 + snapshots.size) % snapshots.size
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
        val profile = allProfiles().firstOrNull { it.modelId == modelId }
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

    fun startNewUserMeter() {
        val draft = defaultMeterDraft(
            displayName = "New Meter",
            modelId = "custom-meter-${userProfiles.size + 1}",
            slaveId = repository.getSlaveId()
        )
        refreshUiState(
            selectedEditableUserModelId = null,
            editMeterDraft = draft
        )
    }

    fun updateEditDraftProfileName(value: String) {
        refreshUiState(editMeterDraft = _uiState.value.editMeterDraft.copy(displayName = value))
    }

    fun updateEditDraftModelId(value: String) {
        refreshUiState(editMeterDraft = _uiState.value.editMeterDraft.copy(modelId = value))
    }

    fun updateEditDraftSlaveId(value: String) {
        refreshUiState(editMeterDraft = _uiState.value.editMeterDraft.copy(slaveIdInput = value))
    }

    fun updateEditDraftRegister(index: Int, update: MeterRegisterDraft.() -> MeterRegisterDraft) {
        val state = _uiState.value
        if (index !in state.editMeterDraft.registers.indices) return

        val updatedRegisters = state.editMeterDraft.registers.toMutableList()
        updatedRegisters[index] = updatedRegisters[index].update()
        refreshUiState(editMeterDraft = state.editMeterDraft.copy(registers = updatedRegisters))
    }

    fun addEditDraftRegister() {
        val state = _uiState.value
        val nextIndex = state.editMeterDraft.registers.size + 1
        val updatedRegisters = state.editMeterDraft.registers + MeterRegisterDraft(
            name = "Register $nextIndex",
            addressInput = "",
            gainInput = "1",
            unit = "",
            initialRawValueInput = "0",
            dataType = DataType.INT16
        )
        refreshUiState(editMeterDraft = state.editMeterDraft.copy(registers = updatedRegisters))
    }

    fun saveNewUserMeter(): Boolean {
        val state = _uiState.value
        val profile = buildProfileFromDraft(state.editMeterDraft) ?: return false

        val duplicateExists = allProfiles().any { it.modelId == profile.modelId }
        if (duplicateExists) {
            logger.error("Model ID already exists: ${profile.modelId}")
            return false
        }

        userProfiles += profile
        logger.info("Added user meter: ${profile.displayName}")

        refreshUiState(
            selectedEditableUserModelId = null,
            editMeterDraft = defaultMeterDraft(
                displayName = "New Meter",
                modelId = "custom-meter-${userProfiles.size + 1}",
                slaveId = repository.getSlaveId()
            )
        )
        return true
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

    private fun buildProfileFromDraft(draft: MeterEditorDraft): MeterProfile? {
        val displayName = draft.displayName.trim()
        val modelId = draft.modelId.trim()
        val slaveId = draft.slaveIdInput.toIntOrNull()

        if (displayName.isBlank()) {
            logger.error("Profile name is required")
            return null
        }
        if (modelId.isBlank()) {
            logger.error("Model ID is required")
            return null
        }
        if (slaveId == null || slaveId !in 1..247) {
            logger.error("Draft Slave ID must be in range 1..247")
            return null
        }

        val points = draft.registers.mapIndexed { index, register ->
            val address = register.addressInput.toIntOrNull()
            val gain = register.gainInput.toIntOrNull()
            val initialRawValue = register.initialRawValueInput.toIntOrNull()

            if (register.name.isBlank() || address == null || gain == null || initialRawValue == null) {
                logger.error("Register ${index + 1} has invalid values")
                return null
            }

            MeterPoint(
                name = register.name,
                address = address,
                registerCount = 1,
                gain = gain,
                dataType = register.dataType,
                unit = register.unit,
                initialRawValue = initialRawValue
            )
        }

        return MeterProfile(
            modelId = modelId,
            displayName = displayName,
            slaveId = slaveId,
            baudRate = 19200,
            dataBits = 8,
            parity = 2,
            stopBits = 1,
            functionCode = 0x03,
            points = points
        )
    }

    private fun refreshUiState(
        selectedPointIndex: Int = _uiState.value.selectedPointIndex,
        rawValueInput: String? = null,
        slaveIdInput: String? = null,
        selectedEditableUserModelId: String? = _uiState.value.selectedEditableUserModelId,
        editMeterDraft: MeterEditorDraft = _uiState.value.editMeterDraft
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
            builtinProfiles = builtinProfiles,
            userProfiles = userProfiles.toList(),
            slaveId = repository.getSlaveId(),
            slaveIdInput = slaveIdInput ?: repository.getSlaveId().toString(),
            points = snapshots,
            selectedPointIndex = safeIndex,
            selectedPoint = selectedPoint,
            rawValueInput = rawValueInput ?: selectedPoint?.rawValue?.toString().orEmpty(),
            selectedEditableUserModelId = selectedEditableUserModelId,
            editMeterDraft = editMeterDraft
        )
    }

    private fun createUiState(): MainUiState {
        val snapshots = repository.snapshot()
        val selectedPoint = snapshots.firstOrNull()
        return MainUiState(
            profileName = repository.getProfile().displayName,
            profileModelId = repository.getProfile().modelId,
            builtinProfiles = builtinProfiles,
            userProfiles = userProfiles.toList(),
            slaveId = repository.getSlaveId(),
            slaveIdInput = repository.getSlaveId().toString(),
            points = snapshots,
            selectedPointIndex = 0,
            selectedPoint = selectedPoint,
            rawValueInput = selectedPoint?.rawValue?.toString().orEmpty(),
            selectedEditableUserModelId = null,
            editMeterDraft = defaultMeterDraft(
                displayName = "New Meter",
                modelId = "custom-meter-1",
                slaveId = repository.getSlaveId()
            )
        )
    }

    private fun defaultMeterDraft(
        displayName: String,
        modelId: String,
        slaveId: Int
    ): MeterEditorDraft {
        return MeterEditorDraft(
            displayName = displayName,
            modelId = modelId,
            slaveIdInput = slaveId.toString(),
            registers = listOf(
                MeterRegisterDraft("Phase A Current", "768", "1", "A", "15"),
                MeterRegisterDraft("Phase B Current", "769", "1", "A", "16"),
                MeterRegisterDraft("Phase C Current", "770", "1", "A", "14"),
                MeterRegisterDraft("Line Voltage A-B", "778", "1", "V", "210"),
                MeterRegisterDraft("Line Voltage B-C", "779", "1", "V", "211"),
                MeterRegisterDraft("Line Voltage C-A", "780", "1", "V", "209"),
                MeterRegisterDraft("Active Power", "794", "1", "W", "5200"),
                MeterRegisterDraft("Import Active Energy Total", "1304", "100", "kWh", "12345"),
                MeterRegisterDraft("Export Active Energy Total", "1306", "100", "kWh", "67")
            )
        )
    }

    private fun allProfiles(): List<MeterProfile> = builtinProfiles + userProfiles

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
    val builtinProfiles: List<MeterProfile>,
    val userProfiles: List<MeterProfile>,
    val slaveId: Int,
    val slaveIdInput: String,
    val points: List<MeterValueSnapshot>,
    val selectedPointIndex: Int,
    val selectedPoint: MeterValueSnapshot?,
    val rawValueInput: String,
    val selectedEditableUserModelId: String?,
    val editMeterDraft: MeterEditorDraft
) {
    val allProfiles: List<MeterProfile>
        get() = builtinProfiles + userProfiles
}

data class MeterEditorDraft(
    val displayName: String,
    val modelId: String,
    val slaveIdInput: String,
    val registers: List<MeterRegisterDraft>
)

data class MeterRegisterDraft(
    val name: String,
    val addressInput: String,
    val gainInput: String,
    val unit: String,
    val initialRawValueInput: String,
    val dataType: DataType = DataType.INT16
)

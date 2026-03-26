package com.example.meterdemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.CommLogger
import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.model.StandardSignalTemplates
import com.example.meterdemo.meter.model.WordByteOrder
import com.example.meterdemo.meter.profiles.MeterProfiles
import com.example.meterdemo.meter.repository.MeterRepository
import com.example.meterdemo.meter.repository.MeterValueSnapshot
import com.example.meterdemo.modbus.ModbusCrc
import com.example.meterdemo.modbus.ModbusFrameParser
import com.example.meterdemo.modbus.ModbusRtuSlaveEngine
import com.example.meterdemo.persistence.MeterPersistence
import com.example.meterdemo.persistence.PersistedMeterState
import com.example.meterdemo.usb.UsbDeviceScanner
import com.example.meterdemo.usb.UsbDeviceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val builtinProfiles = MeterProfiles.all()
    private val userProfiles = mutableListOf<MeterProfile>()
    private val repository = MeterRepository(MeterProfiles.default())
    private val engine = ModbusRtuSlaveEngine(repository)
    private val logger = CommLogger()
    private val persistence = MeterPersistence(application)
    private val usbDeviceScanner = UsbDeviceScanner(application)

    private val _uiState: MutableStateFlow<MainUiState>
    val uiState: StateFlow<MainUiState>
        get() = _uiState.asStateFlow()

    val logs: StateFlow<List<CommLog>> = logger.logs

    init {
        restorePersistedState()
        _uiState = MutableStateFlow(createUiState())
    }

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
        val value = parseRawValueInput(point, state.rawValueInput)

        if (value == null) {
            logger.error("Value does not match ${point.dataType.name}")
            return
        }

        if (!repository.setRawValue(point.address, value)) {
            logger.error("Unsupported address: ${point.address}")
            return
        }

        logger.info("Updated measured value: address=${point.address}, input=${state.rawValueInput}, raw=$value")
        persistState()
        refreshUiState(selectedPointIndex = state.selectedPointIndex)
    }

    fun resetValues() {
        repository.resetCurrentProfileValues()
        logger.info("Reset all register values")
        persistState()
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
        persistState()
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
        persistState()
        refreshUiState(selectedPointIndex = 0)
    }

    fun refreshLogs() {
        logger.info("Refreshed logs")
        refreshUiState(selectedPointIndex = _uiState.value.selectedPointIndex)
    }

    fun refreshUsbDevices() {
        val devices = usbDeviceScanner.scan()
        logger.info("Detected ${devices.size} USB device(s)")
        refreshUiState(
            selectedPointIndex = _uiState.value.selectedPointIndex,
            usbDevices = devices
        )
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
        refreshUiState(
            editingExistingUserMeter = false,
            draftReadOnly = false,
            selectedEditableUserModelId = null,
            draftErrorMessage = null,
            editMeterDraft = defaultMeterDraft(
                displayName = "New Meter",
                modelId = "custom-meter-${userProfiles.size + 1}",
                slaveId = repository.getSlaveId()
            )
        )
    }

    fun startEditingUserMeter(modelId: String) {
        val profile = userProfiles.firstOrNull { it.modelId == modelId }
        if (profile == null) {
            logger.error("User meter not found: $modelId")
            return
        }

        refreshUiState(
            editingExistingUserMeter = true,
            draftReadOnly = false,
            selectedEditableUserModelId = modelId,
            draftErrorMessage = null,
            editMeterDraft = profile.toDraft()
        )
    }

    fun startViewingBuiltinMeter(modelId: String) {
        val profile = builtinProfiles.firstOrNull { it.modelId == modelId }
        if (profile == null) {
            logger.error("Preset not found: $modelId")
            return
        }

        refreshUiState(
            editingExistingUserMeter = false,
            draftReadOnly = true,
            selectedEditableUserModelId = null,
            draftErrorMessage = null,
            editMeterDraft = profile.toDraft()
        )
    }

    fun deleteUserMeters(modelIds: Set<String>) {
        if (modelIds.isEmpty()) return

        userProfiles.removeAll { profile -> modelIds.contains(profile.modelId) }

        if (modelIds.contains(repository.getProfile().modelId)) {
            repository.loadProfile(builtinProfiles.first())
        }

        logger.info("Deleted ${modelIds.size} user meter(s)")
        persistState()
        refreshUiState(
            editingExistingUserMeter = false,
            selectedEditableUserModelId = null
        )
    }

    fun updateEditDraftProfileName(value: String) {
        refreshUiState(
            editMeterDraft = _uiState.value.editMeterDraft.copy(displayName = value),
            draftErrorMessage = null
        )
    }

    fun updateEditDraftModelId(value: String) {
        refreshUiState(
            editMeterDraft = _uiState.value.editMeterDraft.copy(modelId = value),
            draftErrorMessage = null
        )
    }

    fun updateEditDraftSlaveId(value: String) {
        refreshUiState(
            editMeterDraft = _uiState.value.editMeterDraft.copy(slaveIdInput = value),
            draftErrorMessage = null
        )
    }

    fun updateEditDraftFunctionCode(value: Int) {
        refreshUiState(
            editMeterDraft = _uiState.value.editMeterDraft.copy(functionCode = value),
            draftErrorMessage = null
        )
    }

    fun updateEditDraftRegister(index: Int, update: MeterRegisterDraft.() -> MeterRegisterDraft) {
        val state = _uiState.value
        if (index !in state.editMeterDraft.registers.indices) return

        val updatedRegisters = state.editMeterDraft.registers.toMutableList()
        updatedRegisters[index] = updatedRegisters[index].update()
        refreshUiState(
            editMeterDraft = state.editMeterDraft.copy(registers = updatedRegisters),
            draftErrorMessage = null
        )
    }

    fun addEditDraftRegister() {
        val state = _uiState.value
        val nextIndex = state.editMeterDraft.registers.size + 1
        val updatedRegisters = state.editMeterDraft.registers + MeterRegisterDraft(
            name = "Register $nextIndex",
            addressInput = "",
            registerCountInput = "1",
            gainInput = "1",
            unit = "",
            initialRawValueInput = "0",
            dataType = DataType.INT,
            wordByteOrder = WordByteOrder.MSB_MSB
        )
        refreshUiState(
            editMeterDraft = state.editMeterDraft.copy(registers = updatedRegisters),
            draftErrorMessage = null
        )
    }

    fun validateMeterDraft(): Boolean {
        return buildProfileFromDraft(_uiState.value.editMeterDraft) != null
    }

    fun saveMeterDraft(): Boolean {
        val state = _uiState.value
        val profile = buildProfileFromDraft(state.editMeterDraft) ?: return false

        return if (state.editingExistingUserMeter) {
            val originalModelId = state.selectedEditableUserModelId ?: return false
            val duplicateExists = allProfiles().any { it.modelId == profile.modelId && it.modelId != originalModelId }
            if (duplicateExists) {
                reportDraftError("Model ID already exists: ${profile.modelId}")
                false
            } else {
                val index = userProfiles.indexOfFirst { it.modelId == originalModelId }
                if (index == -1) {
                    logger.error("User meter not found")
                    false
                } else {
                    userProfiles[index] = profile
                    if (repository.getProfile().modelId == originalModelId) {
                        repository.loadProfile(profile)
                    }
                    logger.info("Updated user meter: ${profile.displayName}")
                    persistState()
                    refreshUiState(
                        editingExistingUserMeter = true,
                        selectedEditableUserModelId = profile.modelId,
                        draftErrorMessage = null,
                        editMeterDraft = profile.toDraft()
                    )
                    true
                }
            }
        } else {
            val duplicateExists = allProfiles().any { it.modelId == profile.modelId }
            if (duplicateExists) {
                reportDraftError("Model ID already exists: ${profile.modelId}")
                false
            } else {
                userProfiles += profile
                logger.info("Added user meter: ${profile.displayName}")
                persistState()
                refreshUiState(
                    editingExistingUserMeter = false,
                    selectedEditableUserModelId = null,
                    draftErrorMessage = null,
                    editMeterDraft = defaultMeterDraft(
                        displayName = "New Meter",
                        modelId = "custom-meter-${userProfiles.size + 1}",
                        slaveId = repository.getSlaveId()
                    )
                )
                true
            }
        }
    }

    private fun simulateRead(address: Int) {
        val registerCount = repository.findPoint(address)?.registerCount ?: 1
        val requestWithoutCrc = byteArrayOf(
            repository.getSlaveId().toByte(),
            repository.getFunctionCode().toByte(),
            ((address shr 8) and 0xFF).toByte(),
            (address and 0xFF).toByte(),
            0x00,
            registerCount.toByte()
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
            reportDraftError("Profile name is required")
            return null
        }
        if (modelId.isBlank()) {
            reportDraftError("Model ID is required")
            return null
        }
        if (slaveId == null || slaveId !in 1..247) {
            reportDraftError("Slave ID must be in range 1..247")
            return null
        }
        if (draft.functionCode !in setOf(0x03, 0x04)) {
            reportDraftError("Read function must be 03H or 04H")
            return null
        }

        val points = draft.registers.mapIndexed { index, register ->
            val trimmedAddress = register.addressInput.trim()
            if (trimmedAddress.isEmpty()) {
                return@mapIndexed null
            }

            val address = trimmedAddress.toIntOrNull()
            val registerCount = register.registerCountInput.toIntOrNull()
            val gain = register.gainInput.toDoubleOrNull()
            val initialRawValue = parseRegisterInitialValue(register)

            if (
                register.name.isBlank() ||
                address == null ||
                registerCount == null ||
                gain == null ||
                initialRawValue == null
            ) {
                reportDraftError("Register ${index + 1} has invalid values")
                return null
            }
            if (address !in 0..65535) {
                reportDraftError("Register ${index + 1} address must be in range 0..65535")
                return null
            }
            if (registerCount !in 1..4) {
                reportDraftError("Register ${index + 1} word count must be in range 1..4")
                return null
            }
            if (gain !in 0.0..1_000_000.0) {
                reportDraftError("Register ${index + 1} gain must be in range 0.0..1000000.0")
                return null
            }
            if (register.dataType == DataType.FLOAT && registerCount != 2) {
                reportDraftError("Register ${index + 1} FLOAT requires word count 2")
                return null
            }
            if (register.dataType == DataType.INT && registerCount == 1 && initialRawValue !in Short.MIN_VALUE..Short.MAX_VALUE) {
                reportDraftError("Register ${index + 1} INT with word count 1 must be in range -32768..32767")
                return null
            }

            MeterPoint(
                name = register.name,
                address = address,
                registerCount = registerCount,
                gain = gain,
                dataType = register.dataType,
                wordByteOrder = register.wordByteOrder,
                unit = register.unit,
                initialRawValue = initialRawValue
            )
        }.filterNotNull()

        if (points.isEmpty()) {
            reportDraftError("At least one register address is required")
            return null
        }

        return MeterProfile(
            modelId = modelId,
            displayName = displayName,
            slaveId = slaveId,
            baudRate = 19200,
            dataBits = 8,
            parity = 2,
            stopBits = 1,
            functionCode = draft.functionCode,
            points = points
        )
    }

    private fun refreshUiState(
        selectedPointIndex: Int = _uiState.value.selectedPointIndex,
        rawValueInput: String? = null,
        slaveIdInput: String? = null,
        usbDevices: List<UsbDeviceSummary> = _uiState.value.usbDevices,
        editingExistingUserMeter: Boolean = _uiState.value.editingExistingUserMeter,
        draftReadOnly: Boolean = _uiState.value.draftReadOnly,
        selectedEditableUserModelId: String? = _uiState.value.selectedEditableUserModelId,
        draftErrorMessage: String? = _uiState.value.draftErrorMessage,
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
            usbDevices = usbDevices,
            points = snapshots,
            selectedPointIndex = safeIndex,
            selectedPoint = selectedPoint,
            rawValueInput = rawValueInput ?: selectedPoint?.let { formatRawValueInput(it) }.orEmpty(),
            editingExistingUserMeter = editingExistingUserMeter,
            draftReadOnly = draftReadOnly,
            selectedEditableUserModelId = selectedEditableUserModelId,
            draftErrorMessage = draftErrorMessage,
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
            usbDevices = usbDeviceScanner.scan(),
            points = snapshots,
            selectedPointIndex = 0,
            selectedPoint = selectedPoint,
            rawValueInput = selectedPoint?.let { formatRawValueInput(it) }.orEmpty(),
            editingExistingUserMeter = false,
            draftReadOnly = false,
            selectedEditableUserModelId = null,
            draftErrorMessage = null,
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
            functionCode = 0x03,
            registers = standardRegisterDrafts()
        )
    }

    private fun MeterProfile.toDraft(): MeterEditorDraft {
        return MeterEditorDraft(
            displayName = displayName,
            modelId = modelId,
            slaveIdInput = slaveId.toString(),
            functionCode = functionCode,
            registers = points.map { point ->
                MeterRegisterDraft(
                    name = point.name,
                    addressInput = point.address.toString(),
                    registerCountInput = point.registerCount.toString(),
                    gainInput = formatGain(point.gain),
                    unit = point.unit,
                    initialRawValueInput = formatInitialValue(point),
                    dataType = point.dataType,
                    wordByteOrder = point.wordByteOrder
                )
            }
        )
    }

    private fun allProfiles(): List<MeterProfile> = builtinProfiles + userProfiles

    private fun standardRegisterDrafts(): List<MeterRegisterDraft> {
        return StandardSignalTemplates.all.map { template ->
            MeterRegisterDraft(
                name = template.name,
                addressInput = "",
                registerCountInput = "1",
                gainInput = "1",
                unit = template.unit,
                initialRawValueInput = "0",
                dataType = DataType.INT,
                wordByteOrder = WordByteOrder.MSB_MSB
            )
        }
    }

    private fun formatGain(gain: Double): String {
        return if (gain == gain.toInt().toDouble()) {
            gain.toInt().toString()
        } else {
            gain.toString()
        }
    }

    private fun formatInitialValue(point: MeterPoint): String {
        return point.rawInputValue(point.initialRawValue)
    }

    private fun parseRegisterInitialValue(register: MeterRegisterDraft): Int? {
        return when (register.dataType) {
            DataType.FLOAT -> register.initialRawValueInput.toFloatOrNull()?.toRawBits()
            DataType.INT -> register.initialRawValueInput.toIntOrNull()
        }
    }

    private fun formatRawValueInput(point: MeterValueSnapshot): String {
        return point.displayInputValue
    }

    private fun parseRawValueInput(point: MeterValueSnapshot, input: String): Int? {
        val displayValue = input.toDoubleOrNull() ?: return null
        val scaledValue = if (point.gain <= 1.0) displayValue else displayValue * point.gain

        return when (point.dataType) {
            DataType.FLOAT -> scaledValue.toFloat().toRawBits()
            DataType.INT -> {
                val roundedValue = scaledValue.roundToInt()
                if (point.registerCount == 1) {
                    roundedValue.takeIf { it in Short.MIN_VALUE..Short.MAX_VALUE }
                } else {
                    roundedValue
                }
            }
        }
    }

    private fun reportDraftError(message: String) {
        logger.error(message)
        refreshUiState(draftErrorMessage = message)
    }

    private fun persistState() {
        persistence.saveState(
            PersistedMeterState(
                userProfiles = userProfiles.toList(),
                selectedProfileModelId = repository.getProfile().modelId,
                currentSlaveId = repository.getSlaveId(),
                currentRawValues = repository.snapshot().associate { it.address to it.rawValue }
            )
        )
    }

    private fun restorePersistedState() {
        val persisted = persistence.loadState() ?: return
        userProfiles.clear()
        userProfiles.addAll(persisted.userProfiles)

        val profileToLoad = allProfiles().firstOrNull { it.modelId == persisted.selectedProfileModelId }
            ?: repository.getProfile()
        repository.loadProfile(profileToLoad)

        persisted.currentSlaveId?.let(repository::setSlaveId)
        persisted.currentRawValues.forEach { (address, value) ->
            repository.setRawValue(address, value)
        }
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
    val builtinProfiles: List<MeterProfile>,
    val userProfiles: List<MeterProfile>,
    val slaveId: Int,
    val slaveIdInput: String,
    val usbDevices: List<UsbDeviceSummary>,
    val points: List<MeterValueSnapshot>,
    val selectedPointIndex: Int,
    val selectedPoint: MeterValueSnapshot?,
    val rawValueInput: String,
    val editingExistingUserMeter: Boolean,
    val draftReadOnly: Boolean,
    val selectedEditableUserModelId: String?,
    val draftErrorMessage: String?,
    val editMeterDraft: MeterEditorDraft
) {
    val allProfiles: List<MeterProfile>
        get() = builtinProfiles + userProfiles
}

data class MeterEditorDraft(
    val displayName: String,
    val modelId: String,
    val slaveIdInput: String,
    val functionCode: Int,
    val registers: List<MeterRegisterDraft>
)

data class MeterRegisterDraft(
    val name: String,
    val addressInput: String,
    val registerCountInput: String,
    val gainInput: String,
    val unit: String,
    val initialRawValueInput: String,
    val dataType: DataType = DataType.INT,
    val wordByteOrder: WordByteOrder = WordByteOrder.MSB_MSB
)

package com.example.meterdemo.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meterdemo.BuildConfig
import com.example.meterdemo.R
import com.example.meterdemo.localization.AppLanguage
import com.example.meterdemo.localization.AppLanguageManager
import com.example.meterdemo.logging.CommCategory
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.CommLogger
import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.model.SerialParity
import com.example.meterdemo.meter.model.SignalType
import com.example.meterdemo.meter.model.StandardSignalTemplates
import com.example.meterdemo.meter.model.WordByteOrder
import com.example.meterdemo.meter.profiles.MeterProfiles
import com.example.meterdemo.meter.repository.MeterRepository
import com.example.meterdemo.meter.repository.MeterValueSnapshot
import com.example.meterdemo.meter.simulation.MeterSimulationEngine
import com.example.meterdemo.modbus.ModbusCrc
import com.example.meterdemo.modbus.ModbusFrameParser
import com.example.meterdemo.modbus.ModbusRtuSlaveEngine
import com.example.meterdemo.persistence.MeterPersistence
import com.example.meterdemo.persistence.PersistedMeterState
import com.example.meterdemo.usb.UsbDeviceScanner
import com.example.meterdemo.usb.UsbDeviceSummary
import com.example.meterdemo.usb.UsbSerialConnectionManager
import com.example.meterdemo.usb.UsbSerialDeviceSummary
import com.example.meterdemo.usb.UsbRequestFrameAssembler
import com.example.meterdemo.usb.UsbSerialScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val builtinProfiles = MeterProfiles.all()
    private val userProfiles = mutableListOf<MeterProfile>()
    private val repository = MeterRepository(MeterProfiles.default())
    private val engine = ModbusRtuSlaveEngine(repository)
    private val logger = CommLogger()
    private val persistence = MeterPersistence(application)
    private val appLanguageManager = AppLanguageManager(application)
    private val usbDeviceScanner = UsbDeviceScanner(application)
    private val usbSerialScanner = UsbSerialScanner(application)
    private val usbRequestFrameAssembler = UsbRequestFrameAssembler()
    private val simulationEngine = MeterSimulationEngine()
    private val currentAppVersion = resolveCurrentAppVersion(application.packageManager, application.packageName)
    private var simulationJob: Job? = null
    private var lastSimulationTickElapsedRealtime: Long? = null
    private var mainViewMode: MainViewMode = MainViewMode.CARD
    private var appLanguage: AppLanguage = appLanguageManager.getCurrentLanguage()
    private val usbSerialConnectionManager = UsbSerialConnectionManager(
        context = application,
        listener = object : UsbSerialConnectionManager.Listener {
            override fun onPermissionResult(deviceName: String, granted: Boolean) {
                if (granted) {
                    logger.info("USB permission granted: $deviceName", CommCategory.USB)
                } else {
                    logger.error("USB permission denied: $deviceName", CommCategory.USB)
                }
                refreshUsbDevices()
            }

            override fun onConnected(deviceName: String) {
                val profile = repository.getProfile()
                logger.info(
                    "USB serial connected: $deviceName (${profile.baudRate} 8${parityLabel(profile.parity).first()}${profile.stopBits})",
                    CommCategory.USB
                )
                refreshUiState(
                    selectedPointIndex = _uiState.value.selectedPointIndex,
                    usbConnectionStatus = appString(R.string.usb_status_connected),
                    connectedUsbDeviceName = deviceName
                )
                refreshUsbDevices()
            }

            override fun onDisconnected(deviceName: String?, reason: String) {
                if (deviceName != null) {
                    logger.info("USB serial disconnected: $deviceName ($reason)", CommCategory.USB)
                }
                usbRequestFrameAssembler.clear()
                refreshUiState(
                    selectedPointIndex = _uiState.value.selectedPointIndex,
                    usbConnectionStatus = reason,
                    connectedUsbDeviceName = null
                )
                refreshUsbDevices()
            }

            override fun onDataReceived(data: ByteArray) {
                logger.rx(ModbusFrameParser.toHexString(data), "USB RX", CommCategory.USB)
                appendUsbData(data)
            }

            override fun onError(message: String) {
                logger.error(message, CommCategory.USB)
                refreshUiState(
                    selectedPointIndex = _uiState.value.selectedPointIndex,
                    usbConnectionStatus = appString(R.string.usb_status_error)
                )
            }
        }
    )

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

    fun selectPoint(index: Int) {
        refreshUiState(selectedPointIndex = index)
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
        syncSimulationSeed()
        persistState()
        refreshUiState(selectedPointIndex = state.selectedPointIndex)
    }

    fun resetValues() {
        repository.resetCurrentProfileValues()
        logger.info("Reset all register values")
        syncSimulationSeed()
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

    fun toggleMainViewMode() {
        mainViewMode = mainViewMode.next()
        persistState()
        refreshUiState(selectedPointIndex = _uiState.value.selectedPointIndex)
    }

    fun selectAppLanguage(language: AppLanguage) {
        if (language == appLanguage) return
        val currentState = _uiState.value
        appLanguage = language
        appLanguageManager.setLanguage(language)
        val localizedUsbStatus = when {
            currentState.connectedUsbDeviceName != null -> appString(R.string.usb_status_connected)
            currentState.usbConnectionStatus.contains("Connect", ignoreCase = true) &&
                currentState.usbConnectionStatus.contains("...", ignoreCase = true) -> appString(R.string.usb_status_connecting)
            currentState.usbConnectionStatus.contains("failed", ignoreCase = true) -> appString(R.string.usb_status_connect_failed)
            currentState.usbConnectionStatus.contains("error", ignoreCase = true) -> appString(R.string.usb_status_error)
            currentState.usbConnectionStatus.contains("Ú‘±’†") -> appString(R.string.usb_status_connecting)
            currentState.usbConnectionStatus.contains("Ž¸”s") -> appString(R.string.usb_status_connect_failed)
            currentState.usbConnectionStatus.contains("ƒGƒ‰[") -> appString(R.string.usb_status_error)
            else -> appString(R.string.usb_status_disconnected)
        }
        refreshUiState(
            selectedPointIndex = currentState.selectedPointIndex,
            usbConnectionStatus = localizedUsbStatus
        )
    }

    fun selectProfile(modelId: String) {
        val profile = allProfiles().firstOrNull { it.modelId == modelId }
        if (profile == null) {
            logger.error("Preset not found: $modelId")
            return
        }

        repository.loadProfile(profile)
        logger.info("Switched preset: ${profile.displayName}")
        syncSimulationSeed()
        persistState()
        refreshUiState(selectedPointIndex = 0)
    }

    fun startSimulation() {
        if (simulationJob != null) return

        syncSimulationSeed()
        lastSimulationTickElapsedRealtime = SystemClock.elapsedRealtime()
        logger.info("Started automatic meter simulation")
        refreshUiState(
            selectedPointIndex = _uiState.value.selectedPointIndex,
            simulationRunning = true
        )

        simulationJob = viewModelScope.launch {
            while (isActive) {
                delay(SIMULATION_TICK_MS)
                applySimulationTick()
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        lastSimulationTickElapsedRealtime = null
        logger.info("Stopped automatic meter simulation")
        refreshUiState(
            selectedPointIndex = _uiState.value.selectedPointIndex,
            simulationRunning = false
        )
    }

    fun refreshUsbDevices() {
        val devices = usbDeviceScanner.scan()
        val serialDevices = usbSerialScanner.scan()
        logger.info("Detected ${devices.size} USB device(s)", CommCategory.USB)
        refreshUiState(
            selectedPointIndex = _uiState.value.selectedPointIndex,
            usbDevices = devices,
            usbSerialDevices = serialDevices
        )
    }

    fun requestUsbSerialPermission(deviceName: String) {
        if (!usbSerialConnectionManager.requestPermission(deviceName)) {
            logger.error("Failed to request USB permission: $deviceName", CommCategory.USB)
        } else {
            logger.info("Requested USB permission: $deviceName", CommCategory.USB)
        }
    }

    fun connectUsbSerial(deviceName: String) {
        val profile = repository.getProfile()
        refreshUiState(
            selectedPointIndex = _uiState.value.selectedPointIndex,
            usbConnectionStatus = appString(R.string.usb_status_connecting),
            connectedUsbDeviceName = _uiState.value.connectedUsbDeviceName
        )
        if (!usbSerialConnectionManager.connect(
                deviceName = deviceName,
                baudRate = profile.baudRate,
                parity = profile.parity,
                stopBits = if (profile.stopBits == 2) 2 else 1
            )
        ) {
            refreshUiState(
                selectedPointIndex = _uiState.value.selectedPointIndex,
                usbConnectionStatus = appString(R.string.usb_status_connect_failed),
                connectedUsbDeviceName = null
            )
        }
    }

    fun disconnectUsbSerial() {
        usbSerialConnectionManager.disconnect()
    }

    fun checkForAppUpdateOrDownload() {
        val updateState = _uiState.value.appUpdate
        if (updateState.isDownloading) return

        if (updateState.updateAvailable && updateState.apkUrl != null) {
            downloadAndInstallUpdate(updateState.apkUrl, updateState.latestVersionName)
        } else {
            checkForAppUpdate()
        }
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

    fun updateEditDraftBaudRate(value: Int) {
        refreshUiState(
            editMeterDraft = _uiState.value.editMeterDraft.copy(baudRate = value),
            draftErrorMessage = null
        )
    }

    fun updateEditDraftParity(value: SerialParity) {
        refreshUiState(
            editMeterDraft = _uiState.value.editMeterDraft.copy(parity = value),
            draftErrorMessage = null
        )
    }

    fun updateEditDraftStopBits(value: Int) {
        refreshUiState(
            editMeterDraft = _uiState.value.editMeterDraft.copy(stopBits = value),
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
            signalType = SignalType.CUSTOM,
            isTemplateLocked = false,
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
                    syncSimulationSeed()
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
                syncSimulationSeed()
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
        if (draft.baudRate !in SUPPORTED_BAUD_RATES) {
            reportDraftError("Baud rate must be one of ${SUPPORTED_BAUD_RATES.joinToString()}")
            return null
        }
        if (draft.stopBits !in setOf(1, 2)) {
            reportDraftError("Stop bits must be 1 or 2")
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
            if (register.dataType == DataType.INT && registerCount == 1 && initialRawValue !in Short.MIN_VALUE..0xFFFF) {
                reportDraftError("Register ${index + 1} INT with word count 1 must be in range -32768..65535")
                return null
            }

            MeterPoint(
                name = register.name,
                address = address,
                registerCount = registerCount,
                gain = gain,
                dataType = register.dataType,
                signalType = register.signalType,
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
            baudRate = draft.baudRate,
            dataBits = 8,
            parity = draft.parity.profileValue,
            stopBits = draft.stopBits,
            functionCode = draft.functionCode,
            points = points
        )
    }

    private fun refreshUiState(
        selectedPointIndex: Int = _uiState.value.selectedPointIndex,
        rawValueInput: String? = null,
        slaveIdInput: String? = null,
        usbDevices: List<UsbDeviceSummary> = _uiState.value.usbDevices,
        usbSerialDevices: List<UsbSerialDeviceSummary> = _uiState.value.usbSerialDevices,
        usbConnectionStatus: String = _uiState.value.usbConnectionStatus,
        connectedUsbDeviceName: String? = _uiState.value.connectedUsbDeviceName,
        appLanguage: AppLanguage = _uiState.value.appLanguage,
        editingExistingUserMeter: Boolean = _uiState.value.editingExistingUserMeter,
        draftReadOnly: Boolean = _uiState.value.draftReadOnly,
        selectedEditableUserModelId: String? = _uiState.value.selectedEditableUserModelId,
        draftErrorMessage: String? = _uiState.value.draftErrorMessage,
        editMeterDraft: MeterEditorDraft = _uiState.value.editMeterDraft,
        simulationRunning: Boolean = _uiState.value.simulationRunning,
        appUpdate: AppUpdateUiState = _uiState.value.appUpdate
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
            profileBaudRate = repository.getProfile().baudRate,
            profileParity = SerialParity.fromProfileValue(repository.getProfile().parity),
            profileStopBits = repository.getProfile().stopBits,
            usbDevices = usbDevices,
            usbSerialDevices = usbSerialDevices,
            usbConnectionStatus = usbConnectionStatus,
            connectedUsbDeviceName = connectedUsbDeviceName,
            appLanguage = appLanguage,
            points = snapshots,
            selectedPointIndex = safeIndex,
            selectedPoint = selectedPoint,
            mainViewMode = mainViewMode,
            rawValueInput = rawValueInput ?: selectedPoint?.let { formatRawValueInput(it) }.orEmpty(),
            simulationRunning = simulationRunning,
            appUpdate = appUpdate,
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
            profileBaudRate = repository.getProfile().baudRate,
            profileParity = SerialParity.fromProfileValue(repository.getProfile().parity),
            profileStopBits = repository.getProfile().stopBits,
            usbDevices = usbDeviceScanner.scan(),
            usbSerialDevices = usbSerialScanner.scan(),
            usbConnectionStatus = appString(R.string.usb_status_disconnected),
            connectedUsbDeviceName = null,
            appLanguage = appLanguage,
            points = snapshots,
            selectedPointIndex = 0,
            selectedPoint = selectedPoint,
            mainViewMode = mainViewMode,
            rawValueInput = selectedPoint?.let { formatRawValueInput(it) }.orEmpty(),
            simulationRunning = false,
            appUpdate = AppUpdateUiState(
                currentVersionName = currentAppVersion.versionName,
                currentVersionCode = currentAppVersion.versionCode,
                statusMessage = appString(R.string.settings_update_ready)
            ),
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
            baudRate = 19200,
            parity = SerialParity.EVEN,
            stopBits = 1,
            registers = standardRegisterDrafts()
        )
    }

    private fun MeterProfile.toDraft(): MeterEditorDraft {
        return MeterEditorDraft(
            displayName = displayName,
            modelId = modelId,
            slaveIdInput = slaveId.toString(),
            functionCode = functionCode,
            baudRate = baudRate,
            parity = SerialParity.fromProfileValue(parity),
            stopBits = stopBits,
            registers = points.map { point ->
                MeterRegisterDraft(
                    name = point.name,
                    signalType = point.signalType,
                    isTemplateLocked = point.signalType != SignalType.CUSTOM,
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
                name = template.signalType.label,
                signalType = template.signalType,
                isTemplateLocked = true,
                addressInput = "",
                registerCountInput = "1",
                gainInput = "1",
                unit = template.signalType.defaultUnit,
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
                    roundedValue.takeIf { it in Short.MIN_VALUE..0xFFFF }
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

    private fun checkForAppUpdate() {
        val currentUpdateState = _uiState.value.appUpdate
        refreshUiState(
            selectedPointIndex = _uiState.value.selectedPointIndex,
            appUpdate = currentUpdateState.copy(
                isChecking = true,
                statusMessage = appString(R.string.update_status_checking)
            )
        )

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchRemoteUpdateInfo(BuildConfig.APP_UPDATE_JSON_URL)
            }

            val nextState = when {
                result == null -> currentUpdateState.copy(
                    isChecking = false,
                    statusMessage = appString(R.string.update_status_failed_fetch)
                )
                result.versionCode > currentAppVersion.versionCode -> currentUpdateState.copy(
                    isChecking = false,
                    updateAvailable = true,
                    latestVersionCode = result.versionCode,
                    latestVersionName = result.versionName,
                    apkUrl = result.apkUrl,
                    statusMessage = appString(R.string.update_status_available, result.versionName)
                )
                else -> currentUpdateState.copy(
                    isChecking = false,
                    updateAvailable = false,
                    latestVersionCode = result.versionCode,
                    latestVersionName = result.versionName,
                    apkUrl = result.apkUrl,
                    statusMessage = appString(R.string.update_status_latest)
                )
            }

            refreshUiState(
                selectedPointIndex = _uiState.value.selectedPointIndex,
                appUpdate = nextState
            )
        }
    }

    private fun downloadAndInstallUpdate(apkUrl: String, latestVersionName: String?) {
        val currentUpdateState = _uiState.value.appUpdate
        refreshUiState(
            selectedPointIndex = _uiState.value.selectedPointIndex,
            appUpdate = currentUpdateState.copy(
                isDownloading = true,
                downloadProgressPercent = 0,
                statusMessage = appString(R.string.update_status_downloading)
            )
        )

        viewModelScope.launch {
            val targetFile = File(getApplication<Application>().filesDir, "updates/meterdemo-update.apk").apply {
                parentFile?.mkdirs()
            }

            val success = withContext(Dispatchers.IO) {
                downloadApk(
                    apkUrl = apkUrl,
                    targetFile = targetFile
                ) { progress ->
                    val current = _uiState.value.appUpdate
                    refreshUiState(
                        selectedPointIndex = _uiState.value.selectedPointIndex,
                        appUpdate = current.copy(
                            isDownloading = true,
                            downloadProgressPercent = progress,
                            statusMessage = if (progress >= 0) {
                                appString(R.string.update_status_downloading_progress, progress)
                            } else {
                                appString(R.string.update_status_downloading)
                            }
                        )
                    )
                }
            }

            if (!success) {
                val failedState = _uiState.value.appUpdate.copy(
                    isDownloading = false,
                    statusMessage = appString(R.string.update_status_failed_download)
                )
                refreshUiState(
                    selectedPointIndex = _uiState.value.selectedPointIndex,
                    appUpdate = failedState
                )
                return@launch
            }

            val readyState = _uiState.value.appUpdate.copy(
                isDownloading = false,
                downloadProgressPercent = 100,
                statusMessage = appString(
                    R.string.update_status_downloaded,
                    latestVersionName ?: appString(R.string.update_generic_label)
                )
            )
            refreshUiState(
                selectedPointIndex = _uiState.value.selectedPointIndex,
                appUpdate = readyState
            )
            launchApkInstaller(targetFile)
        }
    }

    private fun fetchRemoteUpdateInfo(jsonUrl: String): RemoteUpdateInfo? {
        return runCatching {
            val connection = (URL(jsonUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                doInput = true
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return null
            }
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val json = JSONObject(responseText)
            RemoteUpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.optString("versionName").ifBlank { json.getInt("versionCode").toString() },
                apkUrl = json.getString("apkUrl")
            )
        }.getOrNull()
    }

    private fun downloadApk(
        apkUrl: String,
        targetFile: File,
        onProgress: (Int) -> Unit
    ): Boolean {
        return runCatching {
            val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                doInput = true
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return false
            }

            val contentLength = connection.contentLength
            connection.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesCopied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (contentLength > 0) {
                            val progress = ((bytesCopied * 100) / contentLength).toInt().coerceIn(0, 100)
                            onProgress(progress)
                        } else {
                            onProgress(-1)
                        }
                    }
                    output.flush()
                }
            }
            connection.disconnect()
            true
        }.getOrElse {
            false
        }
    }

    private fun launchApkInstaller(apkFile: File) {
        val application = getApplication<Application>()
        val packageManager = application.packageManager
        if (!packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${application.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(settingsIntent)
            val current = _uiState.value.appUpdate
            refreshUiState(
                selectedPointIndex = _uiState.value.selectedPointIndex,
                appUpdate = current.copy(
                    statusMessage = appString(R.string.update_status_allow_install)
                )
            )
            return
        }

        val apkUri = FileProvider.getUriForFile(
            application,
            "${application.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        application.startActivity(installIntent)
    }

    private fun persistState() {
        persistence.saveState(
            PersistedMeterState(
                userProfiles = userProfiles.toList(),
                selectedProfileModelId = repository.getProfile().modelId,
                currentSlaveId = repository.getSlaveId(),
                currentRawValues = repository.snapshot().associate { it.address to it.rawValue },
                mainViewMode = mainViewMode
            )
        )
    }

    private fun restorePersistedState() {
        val persisted = persistence.loadState() ?: return
        userProfiles.clear()
        userProfiles.addAll(persisted.userProfiles)
        userProfiles.removeAll { it.modelId == REMOVED_BACKUP_CT_EDITABLE_MODEL_ID }
        userProfiles.removeAll { it.modelId == REMOVED_DTSU_EDITABLE_MODEL_ID }
        userProfiles.removeAll { it.modelId == REMOVED_MITSUBISHI_EDITABLE_MODEL_ID }

        val profileToLoad = allProfiles().firstOrNull { it.modelId == persisted.selectedProfileModelId }
            ?: repository.getProfile()
        repository.loadProfile(profileToLoad)

        persisted.currentSlaveId?.let(repository::setSlaveId)
        mainViewMode = persisted.mainViewMode
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

    private fun appendUsbData(data: ByteArray) {
        val assemblyResult = usbRequestFrameAssembler.append(
            data = data,
            expectedSlaveId = repository.getSlaveId(),
            allowedFunctionCodes = setOf(0x03, 0x04)
        )

        if (assemblyResult.droppedNoise.isNotEmpty()) {
            logger.info("Dropped USB noise: ${ModbusFrameParser.toHexString(assemblyResult.droppedNoise)}", CommCategory.USB)
        }

        assemblyResult.frames.forEach { frame ->
            val response = engine.handleRequest(frame)
            if (response == null) {
                logger.error("USB request was not handled: ${ModbusFrameParser.toHexString(frame)}", CommCategory.USB)
            } else if (usbSerialConnectionManager.write(response)) {
                logger.tx(ModbusFrameParser.toHexString(response), "USB TX", CommCategory.USB)
            }
        }
    }

    override fun onCleared() {
        simulationJob?.cancel()
        lastSimulationTickElapsedRealtime = null
        usbSerialConnectionManager.release()
        super.onCleared()
    }

    private companion object {
        private const val REMOVED_BACKUP_CT_EDITABLE_MODEL_ID = "backup-ct-editable"
        private const val REMOVED_DTSU_EDITABLE_MODEL_ID = "dtsu666-hw-editable"
        private const val REMOVED_MITSUBISHI_EDITABLE_MODEL_ID = "mitsubishi-me110ssr-mb-editable"
        private const val SIMULATION_TICK_MS = 1_000L
        val SUPPORTED_BAUD_RATES = listOf(1200, 2400, 4800, 9600, 19200, 115200)
    }

    private fun parityLabel(value: Int): String {
        return SerialParity.fromProfileValue(value).label
    }

    private fun applySimulationTick() {
        val points = repository.getAllPoints()
        if (points.isEmpty()) return

        val now = SystemClock.elapsedRealtime()
        val previous = lastSimulationTickElapsedRealtime ?: now
        lastSimulationTickElapsedRealtime = now
        val elapsedSeconds = ((now - previous).coerceAtLeast(0L).toDouble() / 1000.0).coerceIn(0.0, 5.0)

        val displayValues = simulationEngine.tick(points, elapsedSeconds)
        points.forEach { point ->
            val displayValue = displayValues[point.address] ?: return@forEach
            repository.setRawValue(point.address, displayValueToRawValue(point, displayValue))
        }

        refreshUiState(
            selectedPointIndex = _uiState.value.selectedPointIndex,
            simulationRunning = simulationJob != null
        )
    }

    private fun syncSimulationSeed() {
        simulationEngine.reset(
            points = repository.getAllPoints(),
            displayValues = currentDisplayValues()
        )
    }

    private fun currentDisplayValues(): Map<Int, Double> {
        return repository.getAllPoints().associate { point ->
            point.address to point.displayValue(repository.requireRawValue(point.address))
        }
    }

    private fun displayValueToRawValue(point: MeterPoint, displayValue: Double): Int {
        val scaledValue = if (point.gain <= 1.0) displayValue else displayValue * point.gain

        return when (point.dataType) {
            DataType.FLOAT -> scaledValue.toFloat().toRawBits()
            DataType.INT -> {
                val rounded = scaledValue.roundToInt()
                if (point.registerCount == 1) {
                    rounded.coerceIn(Short.MIN_VALUE.toInt(), 0xFFFF)
                } else {
                    rounded
                }
            }
        }
    }

    private fun appString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }
}

private fun resolveCurrentAppVersion(
    packageManager: PackageManager,
    packageName: String
): InstalledAppVersion {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return InstalledAppVersion(
        versionName = packageInfo.versionName ?: "unknown",
        versionCode = packageInfo.longVersionCode.toInt()
    )
}

data class MainUiState(
    val profileName: String,
    val profileModelId: String,
    val builtinProfiles: List<MeterProfile>,
    val userProfiles: List<MeterProfile>,
    val slaveId: Int,
    val slaveIdInput: String,
    val profileBaudRate: Int,
    val profileParity: SerialParity,
    val profileStopBits: Int,
    val usbDevices: List<UsbDeviceSummary>,
    val usbSerialDevices: List<UsbSerialDeviceSummary>,
    val usbConnectionStatus: String,
    val connectedUsbDeviceName: String?,
    val appLanguage: AppLanguage,
    val points: List<MeterValueSnapshot>,
    val selectedPointIndex: Int,
    val selectedPoint: MeterValueSnapshot?,
    val mainViewMode: MainViewMode,
    val rawValueInput: String,
    val simulationRunning: Boolean,
    val appUpdate: AppUpdateUiState,
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
    val baudRate: Int,
    val parity: SerialParity,
    val stopBits: Int,
    val registers: List<MeterRegisterDraft>
)

data class MeterRegisterDraft(
    val name: String,
    val signalType: SignalType = SignalType.CUSTOM,
    val isTemplateLocked: Boolean = false,
    val addressInput: String,
    val registerCountInput: String,
    val gainInput: String,
    val unit: String,
    val initialRawValueInput: String,
    val dataType: DataType = DataType.INT,
    val wordByteOrder: WordByteOrder = WordByteOrder.MSB_MSB
)

data class AppUpdateUiState(
    val currentVersionName: String,
    val currentVersionCode: Int,
    val updateAvailable: Boolean = false,
    val latestVersionName: String? = null,
    val latestVersionCode: Int? = null,
    val apkUrl: String? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int? = null,
    val statusMessage: String = ""
)

private data class InstalledAppVersion(
    val versionName: String,
    val versionCode: Int
)

private data class RemoteUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String
)


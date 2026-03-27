package com.example.meterdemo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.meterdemo.viewmodel.MainViewModel

private enum class Screen {
    Main,
    Settings,
    Logs,
    LogSummary,
    EditMeter,
    AddMeter
}

@Composable
fun MeterDemoApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Main) }

    when (currentScreen) {
        Screen.Main -> MeterValuesScreen(
            uiState = uiState,
            onOpenSettings = { currentScreen = Screen.Settings },
            onPrevious = viewModel::previousPoint,
            onNext = viewModel::nextPoint,
            onRawValueChange = viewModel::updateSelectedRawValue,
            onApplyValue = viewModel::applySelectedRawValue,
            onResetValues = viewModel::resetValues,
            onSimulateRead = viewModel::simulateReadOfSelectedPoint,
            onStartSimulation = viewModel::startSimulation,
            onStopSimulation = viewModel::stopSimulation
        )

        Screen.Settings -> SettingsScreen(
            uiState = uiState,
            logs = logs,
            onBack = { currentScreen = Screen.Main },
            onProfileSelected = viewModel::selectProfile,
            onOpenEditMeter = { currentScreen = Screen.EditMeter },
            onSlaveIdChange = viewModel::updateSlaveIdInput,
            onApplySlaveId = viewModel::applySlaveId,
            onOpenLogs = { currentScreen = Screen.Logs },
            onRefreshLogs = viewModel::refreshLogs,
            onRefreshUsbDevices = viewModel::refreshUsbDevices,
            onRequestUsbPermission = viewModel::requestUsbSerialPermission,
            onConnectUsbDevice = viewModel::connectUsbSerial,
            onDisconnectUsbDevice = viewModel::disconnectUsbSerial,
            onSimulateRead = viewModel::simulateReadOfSelectedPoint,
            onSimulateCustomRequest = viewModel::simulateCustomRequest
        )

        Screen.Logs -> LogsScreen(
            logs = logs,
            onBack = { currentScreen = Screen.Settings },
            onClearLogs = viewModel::clearLogs,
            onOpenSummary = { currentScreen = Screen.LogSummary }
        )

        Screen.LogSummary -> LogSummaryScreen(
            logs = logs,
            onBack = { currentScreen = Screen.Logs }
        )

        Screen.EditMeter -> EditMeterScreen(
            uiState = uiState,
            onBack = { currentScreen = Screen.Settings },
            onCreateMeter = {
                viewModel.startNewUserMeter()
                currentScreen = Screen.AddMeter
            },
            onEditMeter = { modelId ->
                viewModel.startEditingUserMeter(modelId)
                currentScreen = Screen.AddMeter
            },
            onViewPreset = { modelId ->
                viewModel.startViewingBuiltinMeter(modelId)
                currentScreen = Screen.AddMeter
            },
            onDeleteMeters = viewModel::deleteUserMeters
        )

        Screen.AddMeter -> AddMeterScreen(
            uiState = uiState,
            onBack = { currentScreen = Screen.EditMeter },
            onProfileNameChange = viewModel::updateEditDraftProfileName,
            onModelIdChange = viewModel::updateEditDraftModelId,
            onSlaveIdChange = viewModel::updateEditDraftSlaveId,
            onFunctionCodeChange = viewModel::updateEditDraftFunctionCode,
            onBaudRateChange = viewModel::updateEditDraftBaudRate,
            onParityChange = viewModel::updateEditDraftParity,
            onStopBitsChange = viewModel::updateEditDraftStopBits,
            onRegisterNameChange = { index, value ->
                viewModel.updateEditDraftRegister(index) { copy(name = value) }
            },
            onRegisterAddressChange = { index, value ->
                viewModel.updateEditDraftRegister(index) { copy(addressInput = value) }
            },
            onRegisterCountChange = { index, value ->
                viewModel.updateEditDraftRegister(index) { copy(registerCountInput = value) }
            },
            onRegisterGainChange = { index, value ->
                viewModel.updateEditDraftRegister(index) { copy(gainInput = value) }
            },
            onRegisterUnitChange = { index, value ->
                viewModel.updateEditDraftRegister(index) { copy(unit = value) }
            },
            onRegisterInitialValueChange = { index, value ->
                viewModel.updateEditDraftRegister(index) { copy(initialRawValueInput = value) }
            },
            onRegisterDataTypeChange = { index ->
                viewModel.updateEditDraftRegister(index) { copy(dataType = dataType.next()) }
            },
            onRegisterWordByteOrderChange = { index ->
                viewModel.updateEditDraftRegister(index) { copy(wordByteOrder = wordByteOrder.next()) }
            },
            onValidateBeforeOverwrite = viewModel::validateMeterDraft,
            onAddRegister = viewModel::addEditDraftRegister,
            onApply = {
                if (viewModel.saveMeterDraft()) {
                    currentScreen = Screen.EditMeter
                }
            }
        )
    }
}

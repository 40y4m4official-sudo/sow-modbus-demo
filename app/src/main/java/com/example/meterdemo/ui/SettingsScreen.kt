package com.example.meterdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.meterdemo.R
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.localization.AppLanguage
import com.example.meterdemo.viewmodel.MainViewMode
import com.example.meterdemo.viewmodel.MainUiState

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    logs: List<CommLog>,
    onBack: () -> Unit,
    onProfileSelected: (String) -> Unit,
    onOpenEditMeter: () -> Unit,
    onSlaveIdChange: (String) -> Unit,
    onApplySlaveId: () -> Unit,
    onToggleMainViewMode: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenLogs: () -> Unit,
    onRefreshUsbDevices: () -> Unit,
    onRequestUsbPermission: (String) -> Unit,
    onConnectUsbDevice: (String) -> Unit,
    onDisconnectUsbDevice: () -> Unit,
    onCheckAndDownloadUpdate: () -> Unit,
    onSimulateRead: () -> Unit,
    onSimulateCustomRequest: (String) -> Unit
) {
    var presetExpanded by remember { mutableStateOf(false) }
    var customHex by remember { mutableStateOf("") }
    var languageExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
        .padding(20.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            actions = {
                Box {
                    HeaderIconButton(
                        onClick = { languageExpanded = true },
                        contentDescription = stringResource(R.string.language_button_description)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text(text = language.fixedLabel)
                                },
                                onClick = {
                                    languageExpanded = false
                                    onLanguageSelected(language)
                                }
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_meter_preset),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { presetExpanded = !presetExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(uiState.profileName)
                    }
                    if (presetExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(8.dp)
                        ) {
                            uiState.allProfiles.forEach { profile ->
                                val isSelected = profile.modelId == uiState.profileModelId
                                val isAdded = uiState.userProfiles.any { it.modelId == profile.modelId }

                                Button(
                                    onClick = {
                                        presetExpanded = false
                                        onProfileSelected(profile.modelId)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSelected
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = profile.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isAdded) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                                        shape = RoundedCornerShape(999.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.settings_added_tag),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenEditMeter,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_edit_meter))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.slaveIdInput,
                        onValueChange = onSlaveIdChange,
                        label = { Text(stringResource(R.string.settings_slave_address)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onApplySlaveId,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_apply_slave_address))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onToggleMainViewMode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(
                                R.string.settings_main_view,
                                when (uiState.mainViewMode) {
                                    MainViewMode.CARD -> stringResource(R.string.main_view_card)
                                    MainViewMode.LIST -> stringResource(R.string.main_view_list)
                                }
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_usb_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.usbDevices.isEmpty()) {
                            stringResource(R.string.settings_no_usb_devices)
                        } else {
                            stringResource(R.string.settings_detected_devices, uiState.usbDevices.size)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.settings_profile_serial,
                            uiState.profileBaudRate,
                            uiState.profileParity.label.first().toString(),
                            uiState.profileStopBits
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_connection_status, uiState.usbConnectionStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.connectedUsbDeviceName?.let { deviceName ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_connected_device, deviceName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRefreshUsbDevices,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_refresh_usb_devices))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.usbSerialDevices.isEmpty()) {
                            stringResource(R.string.settings_no_supported_usb_serial)
                        } else {
                            stringResource(R.string.settings_usb_serial_adapters, uiState.usbSerialDevices.size)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (uiState.usbSerialDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        uiState.usbSerialDevices.forEach { device ->
                            val isConnected = uiState.connectedUsbDeviceName == device.deviceName
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = device.displayLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.settings_vid_pid_permission,
                                            device.vendorId.toString(16).padStart(4, '0').uppercase(),
                                            device.productId.toString(16).padStart(4, '0').uppercase(),
                                            device.hasPermission.toString()
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = device.deviceName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onRequestUsbPermission(device.deviceName) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.settings_permission))
                                        }
                                        if (isConnected) {
                                            Button(
                                                onClick = onDisconnectUsbDevice,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(stringResource(R.string.settings_disconnect))
                                            }
                                        } else {
                                            Button(
                                                onClick = { onConnectUsbDevice(device.deviceName) },
                                                modifier = Modifier.weight(1f),
                                                enabled = device.hasPermission
                                            ) {
                                                Text(stringResource(R.string.settings_connect))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    if (uiState.usbDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        uiState.usbDevices.forEach { device ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = device.displayLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = device.deviceName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_logs_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_log_entries, logs.size),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onOpenLogs,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_open_logs))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_comm_test_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.settings_current_item,
                            uiState.selectedPoint?.name ?: "-"
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSimulateRead,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_run_read))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customHex,
                        onValueChange = { customHex = it },
                        label = { Text(stringResource(R.string.settings_custom_hex_request)) },
                        placeholder = { Text(stringResource(R.string.settings_custom_hex_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onSimulateCustomRequest(customHex) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_send_custom_request))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_app_update_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_current_version, uiState.appUpdate.currentVersionName),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.appUpdate.statusMessage.ifBlank { stringResource(R.string.settings_update_ready) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.appUpdate.latestVersionName?.let { latestVersion ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_latest_version, latestVersion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    uiState.appUpdate.downloadProgressPercent?.let { progress ->
                        if (uiState.appUpdate.isDownloading || progress > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_download_progress, progress),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCheckAndDownloadUpdate,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.appUpdate.isChecking && !uiState.appUpdate.isDownloading
                    ) {
                        Text(
                            when {
                                uiState.appUpdate.isDownloading -> stringResource(R.string.settings_downloading)
                                uiState.appUpdate.updateAvailable -> stringResource(R.string.settings_download_and_install_update)
                                uiState.appUpdate.isChecking -> stringResource(R.string.settings_checking)
                                else -> stringResource(R.string.settings_check_for_update)
                            }
                        )
                    }
                }
            }
        }
    }
}



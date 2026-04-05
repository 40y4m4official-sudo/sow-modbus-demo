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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.meterdemo.logging.CommLog
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            onBack = onBack
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
                        text = "Meter Preset",
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
                                                    text = "added",
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
                        Text("Edit Meter")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.slaveIdInput,
                        onValueChange = onSlaveIdChange,
                        label = { Text("Slave Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onApplySlaveId,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Slave Address")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onToggleMainViewMode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Main View: ${uiState.mainViewMode.label}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "USB-RS485",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.usbDevices.isEmpty()) {
                            "No USB devices detected yet."
                        } else {
                            "Detected devices: ${uiState.usbDevices.size}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Profile serial: ${uiState.profileBaudRate} 8${uiState.profileParity.label.first()}${uiState.profileStopBits}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connection: ${uiState.usbConnectionStatus}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.connectedUsbDeviceName?.let { deviceName ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connected device: $deviceName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRefreshUsbDevices,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Refresh USB Devices")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.usbSerialDevices.isEmpty()) {
                            "No supported USB serial adapters detected yet. SH-U11C should appear here as an FTDI serial device when connected."
                        } else {
                            "USB serial adapters: ${uiState.usbSerialDevices.size}"
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
                                        text = "VID:PID ${device.vendorId.toString(16).padStart(4, '0').uppercase()}:${device.productId.toString(16).padStart(4, '0').uppercase()} / permission=${device.hasPermission}",
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
                                            Text("Permission")
                                        }
                                        if (isConnected) {
                                            Button(
                                                onClick = onDisconnectUsbDevice,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Disconnect")
                                            }
                                        } else {
                                            Button(
                                                onClick = { onConnectUsbDevice(device.deviceName) },
                                                modifier = Modifier.weight(1f),
                                                enabled = device.hasPermission
                                            ) {
                                                Text("Connect")
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
                        text = "Logs",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Entries: ${logs.size}",
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
                            Text("Open Logs")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Comm Test",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current item: ${uiState.selectedPoint?.name ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSimulateRead,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Run 03H Read")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customHex,
                        onValueChange = { customHex = it },
                        label = { Text("Custom HEX Request") },
                        placeholder = { Text("02 03 03 00 00 01 44 F8") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onSimulateCustomRequest(customHex) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Custom Request")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Update",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current version: ${uiState.appUpdate.currentVersionName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.appUpdate.statusMessage.ifBlank { "Ready to check for updates" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.appUpdate.latestVersionName?.let { latestVersion ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latest version: $latestVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    uiState.appUpdate.downloadProgressPercent?.let { progress ->
                        if (uiState.appUpdate.isDownloading || progress > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Download progress: $progress%",
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
                                uiState.appUpdate.isDownloading -> "Downloading..."
                                uiState.appUpdate.updateAvailable -> "Download and Install Update"
                                uiState.appUpdate.isChecking -> "Checking..."
                                else -> "Check for Update"
                            }
                        )
                    }
                }
            }
        }
    }
}

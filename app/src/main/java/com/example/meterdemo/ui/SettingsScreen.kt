package com.example.meterdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
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
    onSlaveIdChange: (String) -> Unit,
    onApplySlaveId: () -> Unit,
    onOpenLogs: () -> Unit,
    onRefreshLogs: () -> Unit,
    onSimulateRead: () -> Unit,
    onSimulateCustomRequest: (String) -> Unit
) {
    var presetExpanded by remember { mutableStateOf(false) }
    var customHex by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Meter Preset",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { presetExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(uiState.profileName)
                }
                DropdownMenu(
                    expanded = presetExpanded,
                    onDismissRequest = { presetExpanded = false }
                ) {
                    uiState.profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.displayName) },
                            onClick = {
                                presetExpanded = false
                                onProfileSelected(profile.modelId)
                            }
                        )
                    }
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Logs")
                    }
                    OutlinedButton(
                        onClick = onRefreshLogs,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh Logs")
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
    }
}

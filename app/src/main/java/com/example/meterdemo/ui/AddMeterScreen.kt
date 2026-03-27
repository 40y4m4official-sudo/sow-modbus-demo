package com.example.meterdemo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.SerialParity
import com.example.meterdemo.viewmodel.MainUiState

@Composable
fun AddMeterScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onProfileNameChange: (String) -> Unit,
    onModelIdChange: (String) -> Unit,
    onSlaveIdChange: (String) -> Unit,
    onFunctionCodeChange: (Int) -> Unit,
    onBaudRateChange: (Int) -> Unit,
    onParityChange: (SerialParity) -> Unit,
    onStopBitsChange: (Int) -> Unit,
    onRegisterNameChange: (Int, String) -> Unit,
    onRegisterAddressChange: (Int, String) -> Unit,
    onRegisterCountChange: (Int, String) -> Unit,
    onRegisterGainChange: (Int, String) -> Unit,
    onRegisterUnitChange: (Int, String) -> Unit,
    onRegisterInitialValueChange: (Int, String) -> Unit,
    onRegisterDataTypeChange: (Int) -> Unit,
    onRegisterWordByteOrderChange: (Int) -> Unit,
    onValidateBeforeOverwrite: () -> Boolean,
    onAddRegister: () -> Unit,
    onApply: () -> Unit
) {
    val draft = uiState.editMeterDraft
    var showOverwriteDialog by remember { mutableStateOf(false) }
    val isEditing = uiState.editingExistingUserMeter
    val isReadOnly = uiState.draftReadOnly

    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = { Text("Overwrite meter?") },
            text = { Text("Overwrite the existing user meter with the edited register settings?") },
            confirmButton = {
                Button(onClick = {
                    showOverwriteDialog = false
                    onApply()
                }) {
                    Text("Overwrite")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOverwriteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        ScreenHeader(
            title = if (isReadOnly) "Preset Registers" else if (isEditing) "Register Settings" else "Add Meter",
            trailingText = "Back",
            onTrailingClick = onBack
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (isReadOnly) {
                    "Preset register settings are shown below. To change the active Slave ID, use the Settings screen."
                } else {
                    "The standard 22 signal templates are preloaded. Leave address blank for signals you do not use. Serial settings are saved into each meter profile."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Profile Info",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft.displayName,
                        onValueChange = onProfileNameChange,
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReadOnly
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft.modelId,
                        onValueChange = onModelIdChange,
                        label = { Text("Model ID") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReadOnly
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft.slaveIdInput,
                        onValueChange = onSlaveIdChange,
                        label = { Text("Default Slave ID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReadOnly
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onFunctionCodeChange(
                                if (draft.functionCode == 0x03) 0x04 else 0x03
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReadOnly
                        ) {
                            Text(
                                text = "Read Function: ${if (draft.functionCode == 0x03) "03H Holding Registers" else "04H Input Registers"}"
                            )
                        }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val nextBaudRate = nextBaudRate(draft.baudRate)
                                onBaudRateChange(nextBaudRate)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isReadOnly
                        ) {
                            Text("Baud: ${draft.baudRate}")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { onParityChange(draft.parity.next()) },
                            modifier = Modifier.weight(1f),
                            enabled = !isReadOnly
                        ) {
                            Text("Parity: ${draft.parity.label}")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onStopBitsChange(if (draft.stopBits == 1) 2 else 1) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReadOnly
                    ) {
                        Text("Stop Bit: ${draft.stopBits}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            draft.registers.forEachIndexed { index, register ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Register ${index + 1}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (register.isTemplateLocked) {
                            Text(
                                text = register.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                        } else {
                            OutlinedTextField(
                                value = register.name,
                                onValueChange = { onRegisterNameChange(index, it) },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isReadOnly
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = register.addressInput,
                                onValueChange = { onRegisterAddressChange(index, it) },
                                label = { Text("Address") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                enabled = !isReadOnly
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = register.registerCountInput,
                                onValueChange = { onRegisterCountChange(index, it) },
                                label = { Text("Words") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                enabled = !isReadOnly
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = register.gainInput,
                                onValueChange = { onRegisterGainChange(index, it) },
                                label = { Text("Gain") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                enabled = !isReadOnly
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = register.unit,
                                onValueChange = { onRegisterUnitChange(index, it) },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f),
                                enabled = !isReadOnly
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = register.initialRawValueInput,
                                onValueChange = { onRegisterInitialValueChange(index, it) },
                                label = {
                                    Text(
                                        if (register.dataType == DataType.FLOAT) {
                                            "Initial Float Value"
                                        } else {
                                            "Initial Raw Value"
                                        }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (register.dataType == DataType.FLOAT) {
                                        KeyboardType.Decimal
                                    } else {
                                        KeyboardType.Number
                                    }
                                ),
                                modifier = Modifier.weight(1f),
                                enabled = !isReadOnly
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onRegisterDataTypeChange(index) },
                                modifier = Modifier.weight(1f),
                                enabled = !isReadOnly
                            ) {
                                Text("Type: ${register.dataType.name}")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = { onRegisterWordByteOrderChange(index) },
                                modifier = Modifier.weight(1f),
                                enabled = !isReadOnly
                            ) {
                                Text("Order: ${register.wordByteOrder.label}")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Address blank means unused. Words: 1..4, Gain: 0.0..1000000.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!isReadOnly) {
                OutlinedButton(
                    onClick = onAddRegister,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Register")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isReadOnly) {
            uiState.draftErrorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (!isReadOnly) {
            Button(
                onClick = {
                    if (isEditing) {
                        if (onValidateBeforeOverwrite()) {
                            showOverwriteDialog = true
                        }
                    } else {
                        onApply()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply")
            }
        }
    }
}

private fun nextBaudRate(current: Int): Int {
    val baudRates = listOf(1200, 2400, 4800, 9600, 19200, 115200)
    val currentIndex = baudRates.indexOf(current).takeIf { it >= 0 } ?: 0
    return baudRates[(currentIndex + 1) % baudRates.size]
}

package com.example.meterdemo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.meterdemo.viewmodel.MainUiState

@Composable
fun AddMeterScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onProfileNameChange: (String) -> Unit,
    onModelIdChange: (String) -> Unit,
    onSlaveIdChange: (String) -> Unit,
    onFunctionCodeChange: (Int) -> Unit,
    onRegisterNameChange: (Int, String) -> Unit,
    onRegisterAddressChange: (Int, String) -> Unit,
    onRegisterGainChange: (Int, String) -> Unit,
    onRegisterUnitChange: (Int, String) -> Unit,
    onRegisterInitialValueChange: (Int, String) -> Unit,
    onRegisterDataTypeChange: (Int) -> Unit,
    onRegisterWordByteOrderChange: (Int) -> Unit,
    onAddRegister: () -> Unit,
    onApply: () -> Unit
) {
    val draft = uiState.editMeterDraft
    var showOverwriteDialog by remember { mutableStateOf(false) }
    val isEditing = uiState.editingExistingUserMeter

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
            .padding(20.dp)
    ) {
        ScreenHeader(
            title = if (isEditing) "Register Settings" else "Add Meter",
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
                text = "The standard 22 signal templates are preloaded. Leave address blank for signals you do not use.",
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft.modelId,
                        onValueChange = onModelIdChange,
                        label = { Text("Model ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft.slaveIdInput,
                        onValueChange = onSlaveIdChange,
                        label = { Text("Default Slave ID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onFunctionCodeChange(
                                if (draft.functionCode == 0x03) 0x04 else 0x03
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Read Function: ${if (draft.functionCode == 0x03) "03H Holding Registers" else "04H Input Registers"}"
                        )
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
                        OutlinedTextField(
                            value = register.name,
                            onValueChange = { onRegisterNameChange(index, it) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = register.addressInput,
                                onValueChange = { onRegisterAddressChange(index, it) },
                                label = { Text("Address") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = register.gainInput,
                                onValueChange = { onRegisterGainChange(index, it) },
                                label = { Text("Gain") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = register.unit,
                                onValueChange = { onRegisterUnitChange(index, it) },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = register.initialRawValueInput,
                                onValueChange = { onRegisterInitialValueChange(index, it) },
                                label = { Text("Initial Raw Value") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onRegisterDataTypeChange(index) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Type: ${register.dataType.name}")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = { onRegisterWordByteOrderChange(index) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Order: ${register.wordByteOrder.label}")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Words: ${register.dataType.registerCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = onAddRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Register")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (isEditing) {
                    showOverwriteDialog = true
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

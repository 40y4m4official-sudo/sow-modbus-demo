package com.example.meterdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.meterdemo.viewmodel.MainUiState

@Composable
fun EditMeterScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onCreateMeter: () -> Unit,
    onSelectMeter: (String) -> Unit,
    onProfileNameChange: (String) -> Unit,
    onModelIdChange: (String) -> Unit,
    onSlaveIdChange: (String) -> Unit,
    onRegisterNameChange: (Int, String) -> Unit,
    onRegisterAddressChange: (Int, String) -> Unit,
    onRegisterGainChange: (Int, String) -> Unit,
    onRegisterUnitChange: (Int, String) -> Unit,
    onRegisterInitialValueChange: (Int, String) -> Unit,
    onAddRegister: () -> Unit,
    onSave: () -> Boolean
) {
    val draft = uiState.editMeterDraft

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Edit Meter",
                style = MaterialTheme.typography.headlineMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCreateMeter) {
                    Text("+")
                }
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "User Meters",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState.userProfiles.isEmpty()) {
                        Text("No user meters yet. Use + to create one.")
                    } else {
                        uiState.userProfiles.forEach { profile ->
                            OutlinedButton(
                                onClick = { onSelectMeter(profile.modelId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(profile.displayName)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Preset meters are stored separately and cannot be edited here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Meter Details",
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
                        OutlinedTextField(
                            value = register.addressInput,
                            onValueChange = { onRegisterAddressChange(index, it) },
                            label = { Text("Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = register.gainInput,
                            onValueChange = { onRegisterGainChange(index, it) },
                            label = { Text("Gain") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = register.unit,
                            onValueChange = { onRegisterUnitChange(index, it) },
                            label = { Text("Unit") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = register.initialRawValueInput,
                            onValueChange = { onRegisterInitialValueChange(index, it) },
                            label = { Text("Initial Raw Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onSave() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save User Meter")
            }
        }
    }
}

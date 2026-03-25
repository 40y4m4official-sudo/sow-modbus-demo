package com.example.meterdemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meterdemo.viewmodel.MainUiState

@Composable
fun EditMeterScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onCreateMeter: () -> Unit,
    onEditMeter: (String) -> Unit,
    onDeleteMeters: (Set<String>) -> Unit
) {
    var deleteMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete meters?") },
            text = { Text("Delete ${selectedIds.size} selected user meter(s)?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteMeters(selectedIds)
                    selectedIds = emptySet()
                    deleteMode = false
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Meter",
                    style = MaterialTheme.typography.headlineMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        deleteMode = !deleteMode
                        selectedIds = emptySet()
                    }) {
                        Text("Delete")
                    }
                    OutlinedButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (deleteMode) {
                    "Select the user meters to remove."
                } else {
                    "Tap a user meter to open its register settings."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.userProfiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No added meters yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.userProfiles) { profile ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (deleteMode) {
                                        selectedIds = if (selectedIds.contains(profile.modelId)) {
                                            selectedIds - profile.modelId
                                        } else {
                                            selectedIds + profile.modelId
                                        }
                                    } else {
                                        onEditMeter(profile.modelId)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (deleteMode) {
                                    Checkbox(
                                        checked = selectedIds.contains(profile.modelId),
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) {
                                                selectedIds + profile.modelId
                                            } else {
                                                selectedIds - profile.modelId
                                            }
                                        }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = profile.modelId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!deleteMode) {
                                    Text(
                                        text = "Edit",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FilledTonalButton(
            onClick = {
                if (deleteMode) {
                    if (selectedIds.isNotEmpty()) {
                        showDeleteDialog = true
                    }
                } else {
                    onCreateMeter()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            enabled = !deleteMode || selectedIds.isNotEmpty(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(if (deleteMode) "Delete Selected Meters" else "Add Meter")
        }
    }
}

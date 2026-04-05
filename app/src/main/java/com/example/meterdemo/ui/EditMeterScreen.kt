package com.example.meterdemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import com.example.meterdemo.R
import com.example.meterdemo.viewmodel.MainUiState

@Composable
fun EditMeterScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onCreateMeter: () -> Unit,
    onEditMeter: (String) -> Unit,
    onViewPreset: (String) -> Unit,
    onDeleteMeters: (Set<String>) -> Unit
) {
    var deleteMode by rememberSaveable { mutableStateOf(false) }
    var showPresets by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.edit_meter_delete_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.edit_meter_delete_dialog_message,
                        selectedIds.size
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    onDeleteMeters(selectedIds)
                    selectedIds = emptySet()
                    deleteMode = false
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.edit_meter_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.edit_meter_cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.edit_meter_title),
                onBack = onBack,
                actions = {
                    HeaderIconButton(
                        onClick = {
                            deleteMode = !deleteMode
                            selectedIds = emptySet()
                        },
                        contentDescription = stringResource(R.string.edit_meter_delete_mode)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (deleteMode) {
                    stringResource(R.string.edit_meter_delete_mode_hint)
                } else {
                    stringResource(R.string.edit_meter_default_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.userProfiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.edit_meter_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.userProfiles) { profile ->
                        MeterProfileCard(
                            title = profile.displayName,
                            subtitle = profile.modelId,
                            actionText = if (deleteMode) "" else stringResource(R.string.edit_meter_action_edit),
                            deleteMode = deleteMode,
                            checked = selectedIds.contains(profile.modelId),
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + profile.modelId else selectedIds - profile.modelId
                            },
                            onClick = {
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
                        )
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { showPresets = !showPresets },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showPresets) {
                                stringResource(R.string.edit_meter_hide_presets)
                            } else {
                                stringResource(R.string.edit_meter_show_presets)
                            }
                        )
                    }
                }
                if (showPresets) {
                    items(uiState.builtinProfiles) { profile ->
                        MeterProfileCard(
                            title = profile.displayName,
                            subtitle = profile.modelId,
                            actionText = stringResource(R.string.edit_meter_action_view),
                            deleteMode = false,
                            checked = false,
                            onCheckedChange = {},
                            onClick = { onViewPreset(profile.modelId) }
                        )
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
            Text(
                if (deleteMode) {
                    stringResource(R.string.edit_meter_delete_selected)
                } else {
                    stringResource(R.string.edit_meter_add_meter)
                }
            )
        }
    }
}

@Composable
private fun MeterProfileCard(
    title: String,
    subtitle: String,
    actionText: String,
    deleteMode: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (deleteMode) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!deleteMode && actionText.isNotBlank()) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

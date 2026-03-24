package com.example.meterdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.Direction
import com.example.meterdemo.meter.repository.MeterValueSnapshot
import com.example.meterdemo.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeterValuesScreen(
    viewModel: MainViewModel
) {
    val logs by viewModel.logs.collectAsState()
    val snapshots = viewModel.getSnapshots()

    val inputMap = remember { mutableStateMapOf<Int, String>() }
    var customHex by remember { mutableStateOf("") }

    LaunchedEffect(snapshots) {
        snapshots.forEach { snapshot ->
            if (inputMap[snapshot.address] == null) {
                inputMap[snapshot.address] = snapshot.rawValue.toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = viewModel.getProfileName(),
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.resetValues() }) {
                Text("初期値に戻す")
            }
            OutlinedButton(onClick = { viewModel.clearLogs() }) {
                Text("ログクリア")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "レジスタ一覧",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        snapshots.forEach { snapshot ->
            MeterPointEditorCard(
                snapshot = snapshot,
                inputValue = inputMap[snapshot.address] ?: snapshot.rawValue.toString(),
                onInputChange = { inputMap[snapshot.address] = it },
                onApplyClick = {
                    viewModel.updateRawValue(snapshot.address, inputMap[snapshot.address] ?: "")
                },
                onReadClick = {
                    viewModel.simulateRead(snapshot.address)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "カスタム要求",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customHex,
            onValueChange = { customHex = it },
            label = { Text("HEX 文字列") },
            placeholder = { Text("02 03 03 00 00 01 44 F8") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.simulateCustomRequest(customHex) }
        ) {
            Text("カスタム要求送信")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "通信ログ",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LogList(logs = logs)
    }
}

@Composable
private fun MeterPointEditorCard(
    snapshot: MeterValueSnapshot,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onApplyClick: () -> Unit,
    onReadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = snapshot.name,
                style = MaterialTheme.typography.titleSmall
            )
            Text("Address: ${snapshot.address}")
            Text("Raw: ${snapshot.rawValue}")
            Text("Display: ${snapshot.formattedValue}")

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputValue,
                onValueChange = onInputChange,
                label = { Text("Raw Value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApplyClick) {
                    Text("値を反映")
                }
                OutlinedButton(onClick = onReadClick) {
                    Text("03H読取")
                }
            }
        }
    }
}

@Composable
private fun LogList(logs: List<CommLog>) {
    Column {
        if (logs.isEmpty()) {
            Text("ログはまだありません")
            return
        }

        logs.forEach { log ->
            LogRow(log)
            Divider()
        }
    }
}

@Composable
private fun LogRow(log: CommLog) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val timeText = formatter.format(Date(log.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "[$timeText] ${log.direction.name}",
            style = MaterialTheme.typography.labelMedium
        ) 
        if (log.hex.isNotBlank()) {
            Text(
                text = log.hex,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (log.note.isNotBlank()) {
            Text(
                text = log.note,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
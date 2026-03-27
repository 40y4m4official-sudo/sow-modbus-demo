package com.example.meterdemo.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meterdemo.logging.CommLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    logs: List<CommLog>,
    onBack: () -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        ScreenHeader(
            title = "Communication Logs",
            trailingText = "Back",
            onTrailingClick = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.End)
        ) {
            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Meter Demo Logs")
                        putExtra(Intent.EXTRA_TEXT, buildLogExportText(logs))
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export logs"))
                },
                enabled = logs.isNotEmpty()
            ) {
                Text("Export Logs")
            }
            OutlinedButton(onClick = onClearLogs) {
                Text("Clear Logs")
            }
        }

        if (logs.isEmpty()) {
            Text("No logs yet")
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logs) { log ->
                LogRow(log = log)
                HorizontalDivider()
            }
        }
    }
}

private fun buildLogExportText(logs: List<CommLog>): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    return logs.asReversed().joinToString(separator = "\n") { log ->
        buildString {
            append("[")
            append(formatter.format(Date(log.timestamp)))
            append("] ")
            append(log.direction.name)
            if (log.hex.isNotBlank()) {
                append(" ")
                append(log.hex)
            }
            if (log.note.isNotBlank()) {
                append(" | ")
                append(log.note)
            }
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
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "[$timeText] ${log.direction.name}",
            style = MaterialTheme.typography.labelLarge
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

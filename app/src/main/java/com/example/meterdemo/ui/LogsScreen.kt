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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.meterdemo.R
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.LogExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    logs: List<CommLog>,
    onBack: () -> Unit,
    onClearLogs: () -> Unit,
    onOpenSummary: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.logs_title),
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.End)
        ) {
            Text(
                text = stringResource(R.string.logs_stored, logs.size),
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onOpenSummary,
                enabled = logs.isNotEmpty()
            ) {
                Text(stringResource(R.string.logs_summary))
            }
            HeaderIconButton(
                onClick = {
                    val exportUri = LogExporter.exportLogs(context, logs)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.logs_share_subject))
                        putExtra(Intent.EXTRA_STREAM, exportUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.logs_export_chooser)))
                },
                contentDescription = stringResource(R.string.logs_export_description)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileUpload,
                    contentDescription = null
                )
            }
            HeaderIconButton(
                onClick = onClearLogs,
                contentDescription = stringResource(R.string.logs_clear_description)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = null
                )
            }
        }

        if (logs.isEmpty()) {
            Text(stringResource(R.string.logs_empty))
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
            text = "[$timeText] ${log.category.name} / ${log.direction.name}",
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

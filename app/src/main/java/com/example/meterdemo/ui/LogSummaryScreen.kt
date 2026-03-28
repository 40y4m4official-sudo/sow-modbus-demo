package com.example.meterdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meterdemo.logging.AddressSummary
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.LogAddressSummaryAnalyzer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogSummaryScreen(
    logs: List<CommLog>,
    onBack: () -> Unit
) {
    val summaries = remember(logs) { LogAddressSummaryAnalyzer.summarize(logs) }
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        ScreenHeader(
            title = "Address Summary",
            onBack = onBack
        )

        Text(
            text = "Valid requests: ${summaries.sumOf { it.count }} / unique starts: ${summaries.size}",
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (summaries.isEmpty()) {
            Text("No valid 03H/04H request frames found in current logs.")
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(summaries) { summary ->
                AddressSummaryRow(summary = summary, formatter = formatter)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AddressSummaryRow(
    summary: AddressSummary,
    formatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Addr ${summary.startAddress}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "x${summary.count}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Slave ${summary.slaveId} / FC ${"%02X".format(summary.functionCode)} / Qty ${summary.quantity}",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Last seen ${formatter.format(Date(summary.lastSeenTimestamp))}",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.sampleRequestHex,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

package com.example.meterdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.meterdemo.R
import com.example.meterdemo.analysis.AbnormalType
import com.example.meterdemo.analysis.CommunicationAbnormalEvent
import com.example.meterdemo.analysis.CommunicationAnalysisSnapshot
import com.example.meterdemo.analysis.IntervalJitter
import com.example.meterdemo.analysis.SlaveCommunicationSummary
import com.example.meterdemo.analysis.SlaveStatus
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AnalysisTab {
    DASHBOARD,
    ADDRESS,
    ABNORMAL,
    QUALITY,
    LOG
}

@Composable
fun CommunicationAnalysisScreen(
    uiState: MainUiState,
    logs: List<CommLog>,
    onOpenSettings: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(AnalysisTab.DASHBOARD) }
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val analysis = uiState.communicationAnalysis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.analysis_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(R.string.analysis_passive_mode_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HeaderIconButton(
                onClick = onOpenSettings,
                contentDescription = stringResource(R.string.settings_title)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalysisMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.analysis_metric_frames),
                        value = analysis.totalFrames.toString()
                    )
                    AnalysisMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.analysis_metric_crc),
                        value = analysis.crcErrorCount.toString()
                    )
                    AnalysisMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.analysis_metric_timeout),
                        value = analysis.timeoutCount.toString()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.analysis_connection_line,
                        connectionStatusLabel(uiState),
                        uiState.connectedUsbDeviceName ?: "-"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalysisTabButton(
                modifier = Modifier.weight(1f),
                selected = currentTab == AnalysisTab.DASHBOARD,
                label = stringResource(R.string.analysis_tab_dashboard)
            ) { currentTab = AnalysisTab.DASHBOARD }
            AnalysisTabButton(
                modifier = Modifier.weight(1f),
                selected = currentTab == AnalysisTab.ADDRESS,
                label = stringResource(R.string.analysis_tab_address)
            ) { currentTab = AnalysisTab.ADDRESS }
            AnalysisTabButton(
                modifier = Modifier.weight(1f),
                selected = currentTab == AnalysisTab.ABNORMAL,
                label = stringResource(R.string.analysis_tab_abnormal)
            ) { currentTab = AnalysisTab.ABNORMAL }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalysisTabButton(
                modifier = Modifier.weight(1f),
                selected = currentTab == AnalysisTab.QUALITY,
                label = stringResource(R.string.analysis_tab_quality)
            ) { currentTab = AnalysisTab.QUALITY }
            AnalysisTabButton(
                modifier = Modifier.weight(1f),
                selected = currentTab == AnalysisTab.LOG,
                label = stringResource(R.string.analysis_tab_log)
            ) { currentTab = AnalysisTab.LOG }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (currentTab) {
            AnalysisTab.DASHBOARD -> DashboardTab(analysis = analysis, formatter = formatter)
            AnalysisTab.ADDRESS -> AddressTab(analysis = analysis, formatter = formatter)
            AnalysisTab.ABNORMAL -> AbnormalTab(analysis = analysis, formatter = formatter)
            AnalysisTab.QUALITY -> QualityTab(analysis = analysis)
            AnalysisTab.LOG -> FullLogTab(logs = logs, formatter = formatter)
        }
    }
}

@Composable
private fun DashboardTab(
    analysis: CommunicationAnalysisSnapshot,
    formatter: SimpleDateFormat
) {
    if (analysis.slaveSummaries.isEmpty()) {
        EmptyAnalysisState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalysisMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analysis_metric_slaves),
                    value = analysis.slaveSummaries.count { it.slaveId != 0 }.toString()
                )
                AnalysisMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analysis_metric_abnormal),
                    value = analysis.abnormalEvents.size.toString()
                )
            }
        }

        items(analysis.slaveSummaries) { summary ->
            SummaryRow(summary = summary, formatter = formatter, compact = true)
        }
    }
}

@Composable
private fun AddressTab(
    analysis: CommunicationAnalysisSnapshot,
    formatter: SimpleDateFormat
) {
    if (analysis.slaveSummaries.isEmpty()) {
        EmptyAnalysisState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        items(analysis.slaveSummaries) { summary ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryRow(summary = summary, formatter = formatter, compact = false)
                    summary.lastRequestHex?.let { requestHex ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.analysis_last_request_hex, requestHex),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    summary.lastResponseHex?.let { responseHex ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.analysis_last_response_hex, responseHex),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AbnormalTab(
    analysis: CommunicationAnalysisSnapshot,
    formatter: SimpleDateFormat
) {
    if (analysis.abnormalEvents.isEmpty()) {
        EmptyAnalysisState(text = stringResource(R.string.analysis_abnormal_empty))
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(analysis.abnormalEvents) { event ->
            AbnormalRow(event = event, formatter = formatter)
            HorizontalDivider()
        }
    }
}

@Composable
private fun QualityTab(analysis: CommunicationAnalysisSnapshot) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalysisMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analysis_metric_frames),
                    value = analysis.totalFrames.toString()
                )
                AnalysisMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analysis_metric_truncated),
                    value = analysis.truncatedFrameCount.toString()
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    QualityLine(
                        label = stringResource(R.string.analysis_quality_crc),
                        value = analysis.crcErrorCount.toString()
                    )
                    QualityLine(
                        label = stringResource(R.string.analysis_quality_timeout),
                        value = analysis.timeoutCount.toString()
                    )
                    QualityLine(
                        label = stringResource(R.string.analysis_quality_min),
                        value = analysis.minResponseTimeMs?.let { "$it ms" } ?: "-"
                    )
                    QualityLine(
                        label = stringResource(R.string.analysis_quality_avg),
                        value = analysis.avgResponseTimeMs?.let { "$it ms" } ?: "-"
                    )
                    QualityLine(
                        label = stringResource(R.string.analysis_quality_max),
                        value = analysis.maxResponseTimeMs?.let { "$it ms" } ?: "-"
                    )
                    QualityLine(
                        label = stringResource(R.string.analysis_quality_jitter),
                        value = intervalJitterLabel(analysis.intervalJitter)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullLogTab(
    logs: List<CommLog>,
    formatter: SimpleDateFormat
) {
    if (logs.isEmpty()) {
        EmptyAnalysisState(text = stringResource(R.string.logs_empty))
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(logs) { log ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "[${formatter.format(Date(log.timestamp))}] ${log.category.name} / ${log.direction.name}",
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
            HorizontalDivider()
        }
    }
}

@Composable
private fun SummaryRow(
    summary: SlaveCommunicationSummary,
    formatter: SimpleDateFormat,
    compact: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (summary.slaveId == 0) stringResource(R.string.analysis_broadcast_addr) else stringResource(R.string.summary_addr, summary.slaveId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            StatusPill(summary.status)
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.analysis_last_seen,
                summary.lastSeenTimestamp?.let { formatter.format(Date(it)) } ?: "-"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalysisMetric(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analysis_count_success),
                value = metricValue(summary.successCount, summary.slaveId)
            )
            AnalysisMetric(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analysis_count_timeout),
                value = metricValue(summary.timeoutCount, summary.slaveId)
            )
            AnalysisMetric(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analysis_count_crc),
                value = metricValue(summary.crcErrorCount, summary.slaveId)
            )
            AnalysisMetric(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analysis_count_exception),
                value = metricValue(summary.exceptionCount, summary.slaveId)
            )
        }

        if (!compact) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(
                    R.string.analysis_last_request_line,
                    summary.lastRequestSummary ?: "-"
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.analysis_last_response_line,
                    summary.lastResponseSummary ?: "-"
                ),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AbnormalRow(
    event: CommunicationAbnormalEvent,
    formatter: SimpleDateFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = buildString {
                append(formatter.format(Date(event.timestamp)))
                append("  ")
                append(
                    event.slaveId?.let {
                        if (it == 0) "Addr 00" else "Addr %02d".format(it)
                    } ?: "--"
                )
                append("  ")
                append(abnormalTypeLabel(event.type))
            },
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = event.message,
            style = MaterialTheme.typography.bodyMedium
        )
        event.hex?.let { hex ->
            Text(
                text = hex,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalysisMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AnalysisTabButton(
    modifier: Modifier,
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusPill(status: SlaveStatus) {
    val (label, color) = when (status) {
        SlaveStatus.NORMAL -> stringResource(R.string.analysis_status_normal) to Color(0xFF1E8E5A)
        SlaveStatus.NO_RESPONSE -> stringResource(R.string.analysis_status_no_response) to Color(0xFFC05A2B)
        SlaveStatus.WARNING -> stringResource(R.string.analysis_status_warning) to Color(0xFFD48A00)
        SlaveStatus.EXCEPTION -> stringResource(R.string.analysis_status_exception) to Color(0xFF9C2F2F)
        SlaveStatus.BROADCAST -> stringResource(R.string.analysis_status_broadcast) to Color(0xFF5C6BC0)
        SlaveStatus.ACTIVE -> stringResource(R.string.analysis_status_active) to Color(0xFF2F6FB3)
        SlaveStatus.UNKNOWN -> stringResource(R.string.analysis_status_unknown) to MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.16f), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun QualityLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyAnalysisState(text: String = stringResource(R.string.analysis_empty)) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun connectionStatusLabel(uiState: MainUiState): String {
    return when (uiState.usbConnectionStatus) {
        com.example.meterdemo.viewmodel.UsbConnectionStatus.DISCONNECTED -> stringResource(R.string.usb_status_disconnected)
        com.example.meterdemo.viewmodel.UsbConnectionStatus.CONNECTING -> stringResource(R.string.usb_status_connecting)
        com.example.meterdemo.viewmodel.UsbConnectionStatus.CONNECTED -> stringResource(R.string.usb_status_connected)
        com.example.meterdemo.viewmodel.UsbConnectionStatus.CONNECT_FAILED -> stringResource(R.string.usb_status_connect_failed)
        com.example.meterdemo.viewmodel.UsbConnectionStatus.ERROR -> stringResource(R.string.usb_status_error)
    }
}

@Composable
private fun intervalJitterLabel(jitter: IntervalJitter): String {
    return when (jitter) {
        IntervalJitter.UNKNOWN -> stringResource(R.string.analysis_jitter_unknown)
        IntervalJitter.SMALL -> stringResource(R.string.analysis_jitter_small)
        IntervalJitter.MEDIUM -> stringResource(R.string.analysis_jitter_medium)
        IntervalJitter.LARGE -> stringResource(R.string.analysis_jitter_large)
    }
}

@Composable
private fun abnormalTypeLabel(type: AbnormalType): String {
    return when (type) {
        AbnormalType.TIMEOUT -> stringResource(R.string.analysis_abnormal_timeout)
        AbnormalType.CRC_ERROR -> stringResource(R.string.analysis_abnormal_crc)
        AbnormalType.EXCEPTION -> stringResource(R.string.analysis_abnormal_exception)
        AbnormalType.TRUNCATED -> stringResource(R.string.analysis_abnormal_truncated)
        AbnormalType.UNEXPECTED_LENGTH -> stringResource(R.string.analysis_abnormal_unexpected)
    }
}

private fun metricValue(value: Int, slaveId: Int): String {
    return if (slaveId == 0) "-" else value.toString()
}

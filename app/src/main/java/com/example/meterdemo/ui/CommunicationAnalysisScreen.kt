package com.example.meterdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.meterdemo.logging.AddressSummary
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.LogAddressSummaryAnalyzer
import com.example.meterdemo.viewmodel.MainUiState
import com.example.meterdemo.viewmodel.UsbConnectionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AnalysisViewMode {
    ADDRESS,
    ADDRESS_DETAIL,
    ABNORMAL,
    QUALITY,
    LOG
}

@Composable
fun CommunicationAnalysisScreen(
    uiState: MainUiState,
    logs: List<CommLog>,
    onOpenSettings: () -> Unit,
    onClearLogs: () -> Unit
) {
    var currentMode by rememberSaveable { mutableStateOf(AnalysisViewMode.ADDRESS) }
    var selectedSlaveId by rememberSaveable { mutableStateOf<Int?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val analysis = uiState.communicationAnalysis
    val selectedSummary = analysis.slaveSummaries.firstOrNull { it.slaveId == selectedSlaveId }
    val addressDetails = remember(logs, selectedSlaveId) {
        selectedSlaveId?.let { slaveId ->
            LogAddressSummaryAnalyzer.summarize(logs).filter { it.slaveId == slaveId }
        }.orEmpty()
    }

    if (currentMode == AnalysisViewMode.ADDRESS_DETAIL && selectedSummary == null) {
        currentMode = AnalysisViewMode.ADDRESS
    }

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
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.analysis_passive_mode_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HeaderIconButton(
                onClick = onClearLogs,
                contentDescription = stringResource(R.string.logs_clear_description)
            ) {
                Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null)
            }
            HeaderIconButton(
                onClick = onOpenSettings,
                contentDescription = stringResource(R.string.settings_title)
            ) {
                Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactMetric(
                        label = stringResource(R.string.analysis_metric_frames),
                        value = analysis.totalFrames.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetric(
                        label = stringResource(R.string.analysis_metric_crc),
                        value = analysis.crcErrorCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetric(
                        label = stringResource(R.string.analysis_metric_timeout),
                        value = analysis.timeoutCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.analysis_connection_line,
                            connectionStatusLabel(uiState),
                            uiState.connectedUsbDeviceName ?: "-"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        OutlinedButton(onClick = { menuExpanded = true }) {
                            Text(viewModeLabel(currentMode, selectedSummary), style = MaterialTheme.typography.labelLarge)
                            Icon(
                                imageVector = Icons.Outlined.UnfoldMore,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.analysis_tab_address)) },
                                onClick = {
                                    currentMode = AnalysisViewMode.ADDRESS
                                    menuExpanded = false
                                }
                            )
                            if (selectedSummary != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.analysis_tab_address_detail)) },
                                    onClick = {
                                        currentMode = AnalysisViewMode.ADDRESS_DETAIL
                                        menuExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.analysis_tab_abnormal)) },
                                onClick = {
                                    currentMode = AnalysisViewMode.ABNORMAL
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.analysis_tab_quality)) },
                                onClick = {
                                    currentMode = AnalysisViewMode.QUALITY
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.analysis_tab_log)) },
                                onClick = {
                                    currentMode = AnalysisViewMode.LOG
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (currentMode) {
            AnalysisViewMode.ADDRESS -> AddressTab(
                analysis = analysis,
                formatter = formatter,
                onSelectSummary = { summary ->
                    selectedSlaveId = summary.slaveId
                    currentMode = AnalysisViewMode.ADDRESS_DETAIL
                }
            )
            AnalysisViewMode.ADDRESS_DETAIL -> AddressDetailTab(
                summary = selectedSummary,
                summaries = addressDetails,
                formatter = formatter,
                onBackToAddress = { currentMode = AnalysisViewMode.ADDRESS }
            )
            AnalysisViewMode.ABNORMAL -> AbnormalTab(analysis = analysis, formatter = formatter)
            AnalysisViewMode.QUALITY -> QualityTab(analysis = analysis)
            AnalysisViewMode.LOG -> FullLogTab(logs = logs, formatter = formatter)
        }
    }
}

@Composable
private fun AddressTab(
    analysis: CommunicationAnalysisSnapshot,
    formatter: SimpleDateFormat,
    onSelectSummary: (SlaveCommunicationSummary) -> Unit
) {
    if (analysis.slaveSummaries.isEmpty()) {
        EmptyAnalysisState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        items(analysis.slaveSummaries) { summary ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSummary(summary) }
            ) {
                CompactSummaryRow(summary = summary, formatter = formatter)
            }
        }
    }
}

@Composable
private fun AddressDetailTab(
    summary: SlaveCommunicationSummary?,
    summaries: List<AddressSummary>,
    formatter: SimpleDateFormat,
    onBackToAddress: () -> Unit
) {
    if (summary == null) {
        EmptyAnalysisState()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedButton(onClick = onBackToAddress) {
            Text(stringResource(R.string.analysis_back_to_address))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (summary.slaveId == 0) stringResource(R.string.analysis_broadcast_addr) else stringResource(R.string.summary_addr, summary.slaveId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.analysis_last_seen,
                        summary.lastSeenTimestamp?.let { formatter.format(Date(it)) } ?: "-"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (summaries.isEmpty()) {
            EmptyAnalysisState(text = stringResource(R.string.analysis_detail_empty))
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(summaries) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.summary_addr, item.startAddress),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.summary_count, item.count),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.summary_slave_fc_qty, item.slaveId, "%02X".format(item.functionCode), item.quantity),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.summary_last_seen, formatter.format(Date(item.lastSeenTimestamp))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.sampleRequestHex,
                            style = MaterialTheme.typography.bodySmall
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    QualityLine(label = stringResource(R.string.analysis_metric_frames), value = analysis.totalFrames.toString())
                    QualityLine(label = stringResource(R.string.analysis_metric_truncated), value = analysis.truncatedFrameCount.toString())
                    QualityLine(label = stringResource(R.string.analysis_quality_crc), value = analysis.crcErrorCount.toString())
                    QualityLine(label = stringResource(R.string.analysis_quality_timeout), value = analysis.timeoutCount.toString())
                    QualityLine(label = stringResource(R.string.analysis_quality_min), value = analysis.minResponseTimeMs?.let { "$it ms" } ?: "-")
                    QualityLine(label = stringResource(R.string.analysis_quality_avg), value = analysis.avgResponseTimeMs?.let { "$it ms" } ?: "-")
                    QualityLine(label = stringResource(R.string.analysis_quality_max), value = analysis.maxResponseTimeMs?.let { "$it ms" } ?: "-")
                    QualityLine(label = stringResource(R.string.analysis_quality_jitter), value = intervalJitterLabel(analysis.intervalJitter))
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
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "[${formatter.format(Date(log.timestamp))}] ${log.category.name} / ${log.direction.name}",
                    style = MaterialTheme.typography.labelMedium
                )
                if (log.hex.isNotBlank()) {
                    Text(text = log.hex, style = MaterialTheme.typography.bodySmall)
                }
                if (log.note.isNotBlank()) {
                    Text(
                        text = log.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun CompactSummaryRow(
    summary: SlaveCommunicationSummary,
    formatter: SimpleDateFormat
) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (summary.slaveId == 0) stringResource(R.string.analysis_broadcast_addr) else stringResource(R.string.summary_addr, summary.slaveId),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            StatusPill(summary.status)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.analysis_last_seen,
                summary.lastSeenTimestamp?.let { formatter.format(Date(it)) } ?: "-"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.analysis_last_request_line, summary.lastRequestSummary ?: "-"),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.analysis_last_response_line, summary.lastResponseSummary ?: "-"),
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InlineCount(stringResource(R.string.analysis_count_success), metricValue(summary.successCount, summary.slaveId), Modifier.weight(1f))
            InlineCount(stringResource(R.string.analysis_count_timeout), metricValue(summary.timeoutCount, summary.slaveId), Modifier.weight(1f))
            InlineCount(stringResource(R.string.analysis_count_crc), metricValue(summary.crcErrorCount, summary.slaveId), Modifier.weight(1f))
            InlineCount(stringResource(R.string.analysis_count_exception), metricValue(summary.exceptionCount, summary.slaveId), Modifier.weight(1f))
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
                append(event.slaveId?.let { if (it == 0) "Addr 00" else "Addr %02d".format(it) } ?: "--")
                append("  ")
                append(abnormalTypeLabel(event.type))
            },
            style = MaterialTheme.typography.labelLarge
        )
        Text(text = event.message, style = MaterialTheme.typography.bodyMedium)
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
private fun CompactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InlineCount(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = color, style = MaterialTheme.typography.labelSmall)
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
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyAnalysisState(text: String = stringResource(R.string.analysis_empty)) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        UsbConnectionStatus.DISCONNECTED -> stringResource(R.string.usb_status_disconnected)
        UsbConnectionStatus.CONNECTING -> stringResource(R.string.usb_status_connecting)
        UsbConnectionStatus.CONNECTED -> stringResource(R.string.usb_status_connected)
        UsbConnectionStatus.CONNECT_FAILED -> stringResource(R.string.usb_status_connect_failed)
        UsbConnectionStatus.ERROR -> stringResource(R.string.usb_status_error)
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

@Composable
private fun viewModeLabel(
    mode: AnalysisViewMode,
    selectedSummary: SlaveCommunicationSummary?
): String {
    return when (mode) {
        AnalysisViewMode.ADDRESS -> stringResource(R.string.analysis_tab_address)
        AnalysisViewMode.ADDRESS_DETAIL -> if (selectedSummary == null) {
            stringResource(R.string.analysis_tab_address)
        } else {
            stringResource(
                R.string.analysis_tab_address_detail_selected,
                if (selectedSummary.slaveId == 0) "00" else "%02d".format(selectedSummary.slaveId)
            )
        }
        AnalysisViewMode.ABNORMAL -> stringResource(R.string.analysis_tab_abnormal)
        AnalysisViewMode.QUALITY -> stringResource(R.string.analysis_tab_quality)
        AnalysisViewMode.LOG -> stringResource(R.string.analysis_tab_log)
    }
}

private fun metricValue(value: Int, slaveId: Int): String {
    return if (slaveId == 0) "-" else value.toString()
}
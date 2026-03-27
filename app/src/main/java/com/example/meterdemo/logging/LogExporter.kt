package com.example.meterdemo.logging

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExporter {

    fun exportLogs(context: Context, logs: List<CommLog>): Uri {
        val exportDir = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "log_exports"
        ).apply { mkdirs() }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportFile = File(exportDir, "meter_demo_logs_$timestamp.txt")
        exportFile.writeText(buildLogExportText(logs))

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )
    }

    fun buildLogExportText(logs: List<CommLog>): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return logs.asReversed().joinToString(separator = "\n") { log ->
            buildString {
                append("[")
                append(formatter.format(Date(log.timestamp)))
                append("] ")
                append(log.category.name)
                append(" / ")
                append(log.direction.name)
                if (log.hex.isNotBlank()) {
                    append(" / ")
                    append(log.hex)
                }
                if (log.note.isNotBlank()) {
                    append(" | ")
                    append(log.note)
                }
            }
        }
    }
}

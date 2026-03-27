package com.example.meterdemo.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class LogAddressSummaryAnalyzerTest {

    @Test
    fun summarize_reassemblesSplitUsbFrames() {
        val logs = listOf(
            CommLog(
                category = CommCategory.USB,
                direction = Direction.RX,
                hex = "02 03 90",
                note = "USB RX",
                timestamp = 1000L
            ),
            CommLog(
                category = CommCategory.USB,
                direction = Direction.RX,
                hex = "F9 00 04 B9 0B",
                note = "USB RX",
                timestamp = 1001L
            )
        )

        val summaries = LogAddressSummaryAnalyzer.summarize(logs)

        assertEquals(1, summaries.size)
        assertEquals(37113, summaries.first().startAddress)
        assertEquals(4, summaries.first().quantity)
        assertEquals(1, summaries.first().count)
    }

    @Test
    fun summarize_countsMultipleFramesFromConcatenatedLog() {
        val logs = listOf(
            CommLog(
                category = CommCategory.USB,
                direction = Direction.RX,
                hex = "02 03 90 F9 00 04 B9 0B 02 03 91 01 00 04 39 06",
                note = "USB RX",
                timestamp = 1000L
            )
        )

        val summaries = LogAddressSummaryAnalyzer.summarize(logs)

        assertEquals(2, summaries.size)
        assertEquals(setOf(37113, 37121), summaries.map { it.startAddress }.toSet())
    }
}

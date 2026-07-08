package com.example.meterdemo.analysis

import com.example.meterdemo.modbus.ModbusCrc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CommunicationAnalysisTrackerTest {

    @Test
    fun requestAndResponse_updateSuccessAndLatency() {
        val tracker = CommunicationAnalysisTracker(timeoutThresholdMs = 500L)
        val request = frame(0x01, 0x03, 0x00, 0x64, 0x00, 0x02)
        val response = frame(0x01, 0x03, 0x04, 0x00, 0x64, 0x00, 0xC8)

        tracker.onRawBytes(request, timestamp = 1_000L)
        tracker.onRawBytes(response, timestamp = 1_012L)

        val snapshot = tracker.snapshot(now = 1_012L)
        val summary = snapshot.slaveSummaries.first { it.slaveId == 1 }
        assertEquals(2, snapshot.totalFrames)
        assertEquals(1, summary.successCount)
        assertEquals("Read Holding Register 100 len 2", summary.lastRequestSummary)
        assertEquals("00 64 00 C8", summary.lastResponseHex)
        assertEquals(12L, snapshot.minResponseTimeMs)
        assertEquals(12, snapshot.avgResponseTimeMs)
    }

    @Test
    fun nextRequestAfterThreshold_countsTimeout() {
        val tracker = CommunicationAnalysisTracker(timeoutThresholdMs = 200L)
        val requestA = frame(0x02, 0x03, 0x00, 0x10, 0x00, 0x01)
        val requestB = frame(0x02, 0x03, 0x00, 0x20, 0x00, 0x01)

        tracker.onRawBytes(requestA, timestamp = 1_000L)
        tracker.onRawBytes(requestB, timestamp = 1_300L)

        val snapshot = tracker.snapshot(now = 1_300L)
        val summary = snapshot.slaveSummaries.first { it.slaveId == 2 }
        assertEquals(1, snapshot.timeoutCount)
        assertEquals(1, summary.timeoutCount)
        assertEquals(SlaveStatus.ACTIVE, summary.status)
    }

    @Test
    fun invalidCrc_countsAsAbnormal() {
        val tracker = CommunicationAnalysisTracker()
        val broken = byteArrayOf(0x03, 0x03, 0x00, 0x10, 0x00, 0x01, 0x00, 0x00)

        tracker.onRawBytes(broken, timestamp = 5_000L)

        val snapshot = tracker.snapshot(now = 5_000L)
        assertEquals(1, snapshot.crcErrorCount)
        assertEquals(1, snapshot.abnormalEvents.size)
        assertEquals(AbnormalType.CRC_ERROR, snapshot.abnormalEvents.first().type)
    }

    @Test
    fun exceptionResponse_recordsException() {
        val tracker = CommunicationAnalysisTracker()
        val request = frame(0x04, 0x03, 0x00, 0x99, 0x00, 0x01)
        val exception = frame(0x04, 0x83, 0x02)

        tracker.onRawBytes(request, timestamp = 2_000L)
        tracker.onRawBytes(exception, timestamp = 2_010L)

        val snapshot = tracker.snapshot(now = 2_010L)
        val summary = snapshot.slaveSummaries.first { it.slaveId == 4 }
        assertEquals(1, summary.exceptionCount)
        assertEquals(SlaveStatus.EXCEPTION, summary.status)
        assertNotNull(summary.lastResponseSummary)
    }

    private fun frame(vararg bytes: Int): ByteArray {
        return ModbusCrc.appendCrc(bytes.map { it.toByte() }.toByteArray())
    }
}

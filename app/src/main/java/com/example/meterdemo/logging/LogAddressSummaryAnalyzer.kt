package com.example.meterdemo.logging

import com.example.meterdemo.modbus.ModbusCrc
import com.example.meterdemo.modbus.ModbusFrameParser

data class AddressSummary(
    val slaveId: Int,
    val functionCode: Int,
    val startAddress: Int,
    val quantity: Int,
    val count: Int,
    val lastSeenTimestamp: Long,
    val sampleRequestHex: String
)

object LogAddressSummaryAnalyzer {
    private const val FRAME_SIZE = 8

    fun summarize(logs: List<CommLog>): List<AddressSummary> {
        val requests = extractRequests(logs)

        return requests
            .groupBy { listOf(it.slaveId, it.functionCode, it.startAddress, it.quantity) }
            .values
            .map { grouped ->
                val latest = grouped.maxByOrNull { it.timestamp } ?: grouped.first()
                AddressSummary(
                    slaveId = latest.slaveId,
                    functionCode = latest.functionCode,
                    startAddress = latest.startAddress,
                    quantity = latest.quantity,
                    count = grouped.size,
                    lastSeenTimestamp = latest.timestamp,
                    sampleRequestHex = ModbusFrameParser.toHexString(latest.frame)
                )
            }
            .sortedWith(
                compareByDescending<AddressSummary> { it.count }
                    .thenBy { it.startAddress }
                    .thenBy { it.quantity }
            )
    }

    private fun extractRequests(logs: List<CommLog>): List<ParsedRequest> {
        val buffer = mutableListOf<Byte>()
        val results = mutableListOf<ParsedRequest>()
        var lastTimestamp = 0L

        logs.forEach { log ->
            if (log.category != CommCategory.USB || log.direction != Direction.RX) return@forEach

            val bytes = parseHex(log.hex)
            if (bytes.isEmpty()) return@forEach

            buffer.addAll(bytes.toList())
            lastTimestamp = log.timestamp

            while (buffer.size >= FRAME_SIZE) {
                val matchIndex = findFirstValidFrameStart(buffer)
                if (matchIndex >= 0) {
                    if (matchIndex > 0) {
                        buffer.subList(0, matchIndex).clear()
                    }

                    val frame = buffer.take(FRAME_SIZE).toByteArray()
                    val parsed = ModbusFrameParser.parseReadHoldingRegistersRequest(frame)
                    if (parsed != null) {
                        results += ParsedRequest(
                            slaveId = parsed.slaveId,
                            functionCode = parsed.functionCode,
                            startAddress = parsed.startAddress,
                            quantity = parsed.quantity,
                            frame = frame,
                            timestamp = lastTimestamp
                        )
                    }
                    buffer.subList(0, FRAME_SIZE).clear()
                    continue
                }

                val preserveFrom = findPreserveStart(buffer)
                if (preserveFrom > 0) {
                    buffer.subList(0, preserveFrom).clear()
                } else if (preserveFrom < 0) {
                    val keepCount = minOf(FRAME_SIZE - 1, buffer.size)
                    val dropCount = buffer.size - keepCount
                    if (dropCount > 0) {
                        buffer.subList(0, dropCount).clear()
                    }
                } else {
                    break
                }
            }
        }

        return results
    }

    private fun findFirstValidFrameStart(buffer: List<Byte>): Int {
        for (start in 0..buffer.size - FRAME_SIZE) {
            val frame = buffer.subList(start, start + FRAME_SIZE).toByteArray()
            val slaveId = frame[0].toInt() and 0xFF
            val functionCode = frame[1].toInt() and 0xFF
            if (slaveId !in 1..247) continue
            if (functionCode !in setOf(0x03, 0x04)) continue
            if (!ModbusCrc.isValid(frame)) continue
            if (ModbusFrameParser.parseReadHoldingRegistersRequest(frame) == null) continue
            return start
        }
        return -1
    }

    private fun findPreserveStart(buffer: List<Byte>): Int {
        val tailStart = (buffer.size - (FRAME_SIZE - 1)).coerceAtLeast(0)
        for (index in tailStart until buffer.size) {
            val value = buffer[index].toInt() and 0xFF
            if (value in 1..247) {
                return index
            }
        }
        return -1
    }

    private fun parseHex(hex: String): ByteArray {
        val tokens = hex
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }

        return runCatching {
            ByteArray(tokens.size) { index ->
                tokens[index].toInt(16).toByte()
            }
        }.getOrDefault(ByteArray(0))
    }

    private data class ParsedRequest(
        val slaveId: Int,
        val functionCode: Int,
        val startAddress: Int,
        val quantity: Int,
        val frame: ByteArray,
        val timestamp: Long
    )
}

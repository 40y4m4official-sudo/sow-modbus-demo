package com.example.meterdemo.analysis

import com.example.meterdemo.modbus.ModbusCrc
import com.example.meterdemo.modbus.ModbusFrameParser
import kotlin.math.roundToInt

class CommunicationAnalysisTracker(
    private val timeoutThresholdMs: Long = 1_000L,
    private val maxAbnormalEvents: Int = 200
) {
    private val slaveStats = mutableMapOf<Int, MutableSlaveStats>()
    private val pendingRequests = mutableMapOf<Int, PendingRequest>()
    private val abnormalEvents = ArrayDeque<CommunicationAbnormalEvent>()
    private var validFrameCount = 0
    private var crcErrorCount = 0
    private var truncatedFrameCount = 0
    private var timeoutCount = 0
    private var minResponseTimeMs: Long? = null
    private var maxResponseTimeMs: Long? = null
    private var responseTimeTotalMs = 0L
    private var responseTimeSamples = 0
    private var intervalSampleCount = 0
    private var frameIntervalTotalMs = 0L
    private var frameIntervalSquaredTotal = 0.0
    private var lastValidFrameTimestamp: Long? = null

    fun reset() {
        slaveStats.clear()
        pendingRequests.clear()
        abnormalEvents.clear()
        validFrameCount = 0
        crcErrorCount = 0
        truncatedFrameCount = 0
        timeoutCount = 0
        minResponseTimeMs = null
        maxResponseTimeMs = null
        responseTimeTotalMs = 0L
        responseTimeSamples = 0
        intervalSampleCount = 0
        frameIntervalTotalMs = 0L
        frameIntervalSquaredTotal = 0.0
        lastValidFrameTimestamp = null
    }

    fun onDisconnected(timestamp: Long) {
        expirePendingRequests(timestamp, flushAll = true)
    }

    fun onRawBytes(data: ByteArray, timestamp: Long) {
        expirePendingRequests(timestamp, flushAll = false)

        PassiveModbusFrameDecoder.decode(data).forEach { frame ->
            when (frame) {
                is DecodedPassiveFrame.Truncated -> {
                    truncatedFrameCount += 1
                    recordAbnormal(
                        timestamp = timestamp,
                        slaveId = frame.slaveId,
                        type = AbnormalType.TRUNCATED,
                        message = "Frame truncated",
                        hex = ModbusFrameParser.toHexString(frame.raw)
                    )
                }

                is DecodedPassiveFrame.InvalidCrc -> {
                    crcErrorCount += 1
                    frame.slaveId?.let { statsFor(it).crcErrorCount += 1 }
                    recordAbnormal(
                        timestamp = timestamp,
                        slaveId = frame.slaveId,
                        type = AbnormalType.CRC_ERROR,
                        message = "CRC error",
                        hex = ModbusFrameParser.toHexString(frame.raw)
                    )
                }

                is DecodedPassiveFrame.Valid -> handleValidFrame(frame, timestamp)
            }
        }
    }

    fun snapshot(now: Long = System.currentTimeMillis()): CommunicationAnalysisSnapshot {
        expirePendingRequests(now, flushAll = false)

        val summaries = slaveStats.values
            .map { it.toSnapshot() }
            .sortedWith(compareBy<SlaveCommunicationSummary> { if (it.slaveId == 0) 0 else 1 }.thenBy { it.slaveId })

        val averageResponseTime = if (responseTimeSamples > 0) {
            responseTimeTotalMs.toDouble() / responseTimeSamples.toDouble()
        } else {
            null
        }

        val averageInterval = if (intervalSampleCount > 0) {
            frameIntervalTotalMs.toDouble() / intervalSampleCount.toDouble()
        } else {
            null
        }

        val jitterLabel = if (intervalSampleCount < 2 || averageInterval == null || averageInterval <= 0.0) {
            IntervalJitter.UNKNOWN
        } else {
            val variance = (frameIntervalSquaredTotal / intervalSampleCount.toDouble()) - (averageInterval * averageInterval)
            val stddev = kotlin.math.sqrt(variance.coerceAtLeast(0.0))
            val ratio = stddev / averageInterval
            when {
                ratio < 0.15 -> IntervalJitter.SMALL
                ratio < 0.35 -> IntervalJitter.MEDIUM
                else -> IntervalJitter.LARGE
            }
        }

        return CommunicationAnalysisSnapshot(
            capturedAtTimestamp = now,
            totalFrames = validFrameCount,
            crcErrorCount = crcErrorCount,
            truncatedFrameCount = truncatedFrameCount,
            timeoutCount = timeoutCount,
            minResponseTimeMs = minResponseTimeMs,
            avgResponseTimeMs = averageResponseTime?.roundToInt(),
            maxResponseTimeMs = maxResponseTimeMs,
            intervalJitter = jitterLabel,
            slaveSummaries = summaries,
            abnormalEvents = abnormalEvents.toList()
        )
    }

    private fun handleValidFrame(frame: DecodedPassiveFrame.Valid, timestamp: Long) {
        validFrameCount += 1
        updateFrameInterval(timestamp)

        when (frame) {
            is DecodedPassiveFrame.Valid.Request -> {
                val stats = statsFor(frame.slaveId)
                stats.lastSeenTimestamp = timestamp
                stats.lastRequestTimestamp = timestamp
                stats.lastRequestSummary = describeRequest(frame)
                stats.lastRequestHex = ModbusFrameParser.toHexString(frame.raw)

                if (frame.slaveId == 0) {
                    stats.status = SlaveStatus.BROADCAST
                    stats.lastResponseSummary = "Broadcast"
                    stats.lastResponseHex = null
                    return
                }

                pendingRequests[frame.slaveId]?.let { pending ->
                    if (pending.functionCode == frame.functionCode) {
                        registerTimeout(pending, timestamp)
                    }
                }

                pendingRequests[frame.slaveId] = PendingRequest(
                    slaveId = frame.slaveId,
                    functionCode = frame.functionCode,
                    description = describeRequest(frame),
                    requestedAt = timestamp
                )
                stats.status = SlaveStatus.ACTIVE
            }

            is DecodedPassiveFrame.Valid.Response -> {
                val stats = statsFor(frame.slaveId)
                stats.lastSeenTimestamp = timestamp
                val pending = pendingRequests.remove(frame.slaveId)
                stats.lastResponseTimestamp = timestamp
                stats.lastResponseHex = frame.responseDataHex
                stats.lastResponseSummary = describeResponse(frame)

                if (pending != null && pending.functionCode == frame.functionCode) {
                    val responseTime = (timestamp - pending.requestedAt).coerceAtLeast(0L)
                    stats.successCount += 1
                    stats.lastRequestSummary = pending.description
                    stats.status = SlaveStatus.NORMAL
                    registerResponseTime(responseTime)
                } else {
                    stats.status = SlaveStatus.WARNING
                    recordAbnormal(
                        timestamp = timestamp,
                        slaveId = frame.slaveId,
                        type = AbnormalType.UNEXPECTED_LENGTH,
                        message = "Response without matching request",
                        hex = ModbusFrameParser.toHexString(frame.raw)
                    )
                }
            }

            is DecodedPassiveFrame.Valid.ExceptionResponse -> {
                val slaveId = frame.slaveId
                val stats = statsFor(slaveId)
                stats.lastSeenTimestamp = timestamp
                stats.lastResponseTimestamp = timestamp
                stats.exceptionCount += 1
                stats.lastResponseHex = frame.exceptionCodeHex
                stats.lastResponseSummary = "Exception: ${frame.exceptionName}"
                stats.status = SlaveStatus.EXCEPTION

                val pending = pendingRequests.remove(slaveId)
                if (pending != null) {
                    stats.lastRequestSummary = pending.description
                    registerResponseTime((timestamp - pending.requestedAt).coerceAtLeast(0L))
                }

                recordAbnormal(
                    timestamp = timestamp,
                    slaveId = slaveId,
                    type = AbnormalType.EXCEPTION,
                    message = "Exception: ${frame.exceptionName}",
                    hex = ModbusFrameParser.toHexString(frame.raw)
                )
            }

            is DecodedPassiveFrame.Valid.Generic -> {
                val stats = statsFor(frame.slaveId)
                stats.lastSeenTimestamp = timestamp
                val hex = ModbusFrameParser.toHexString(frame.raw)
                if (frame.functionCode and 0x80 == 0 && frame.raw.size == 8) {
                    stats.lastRequestTimestamp = timestamp
                    stats.lastRequestSummary = "FC %02X generic request".format(frame.functionCode)
                    stats.lastRequestHex = hex
                } else {
                    stats.lastResponseTimestamp = timestamp
                    stats.lastResponseSummary = "FC %02X generic frame".format(frame.functionCode)
                    stats.lastResponseHex = hex
                }
                stats.status = if (stats.status == SlaveStatus.UNKNOWN) SlaveStatus.ACTIVE else stats.status
            }
        }
    }

    private fun updateFrameInterval(timestamp: Long) {
        val previous = lastValidFrameTimestamp
        lastValidFrameTimestamp = timestamp
        if (previous == null) return
        val interval = (timestamp - previous).coerceAtLeast(0L)
        intervalSampleCount += 1
        frameIntervalTotalMs += interval
        frameIntervalSquaredTotal += interval.toDouble() * interval.toDouble()
    }

    private fun expirePendingRequests(timestamp: Long, flushAll: Boolean) {
        val expired = pendingRequests.values
            .filter { flushAll || timestamp - it.requestedAt >= timeoutThresholdMs }
            .sortedBy { it.requestedAt }

        expired.forEach { pending ->
            pendingRequests.remove(pending.slaveId)
            registerTimeout(pending, timestamp)
        }
    }

    private fun registerTimeout(pending: PendingRequest, timestamp: Long) {
        timeoutCount += 1
        val stats = statsFor(pending.slaveId)
        stats.timeoutCount += 1
        stats.lastSeenTimestamp = timestamp
        stats.lastRequestSummary = pending.description
        stats.status = SlaveStatus.NO_RESPONSE
        recordAbnormal(
            timestamp = timestamp,
            slaveId = pending.slaveId,
            type = AbnormalType.TIMEOUT,
            message = "Timeout",
            hex = null
        )
    }

    private fun registerResponseTime(responseTimeMs: Long) {
        minResponseTimeMs = minResponseTimeMs?.coerceAtMost(responseTimeMs) ?: responseTimeMs
        maxResponseTimeMs = maxResponseTimeMs?.coerceAtLeast(responseTimeMs) ?: responseTimeMs
        responseTimeTotalMs += responseTimeMs
        responseTimeSamples += 1
    }

    private fun recordAbnormal(
        timestamp: Long,
        slaveId: Int?,
        type: AbnormalType,
        message: String,
        hex: String?
    ) {
        abnormalEvents.addFirst(
            CommunicationAbnormalEvent(
                timestamp = timestamp,
                slaveId = slaveId,
                type = type,
                message = message,
                hex = hex
            )
        )
        while (abnormalEvents.size > maxAbnormalEvents) {
            abnormalEvents.removeLast()
        }
    }

    private fun statsFor(slaveId: Int): MutableSlaveStats {
        return slaveStats.getOrPut(slaveId) { MutableSlaveStats(slaveId = slaveId) }
    }

    private fun describeRequest(frame: DecodedPassiveFrame.Valid.Request): String {
        val functionLabel = when (frame.functionCode) {
            0x03 -> "Read Holding Register"
            0x04 -> "Read Input Register"
            else -> "Function %02X".format(frame.functionCode)
        }
        return "$functionLabel ${frame.startAddress} len ${frame.quantity}"
    }

    private fun describeResponse(frame: DecodedPassiveFrame.Valid.Response): String {
        return when (frame.functionCode) {
            0x03 -> "Holding response ${frame.byteCount} byte"
            0x04 -> "Input response ${frame.byteCount} byte"
            else -> "Response ${frame.byteCount} byte"
        }
    }
}

private object PassiveModbusFrameDecoder {
    fun decode(data: ByteArray): List<DecodedPassiveFrame> {
        if (data.isEmpty()) return emptyList()

        val frames = mutableListOf<DecodedPassiveFrame>()
        var index = 0

        while (index < data.size) {
            val remaining = data.copyOfRange(index, data.size)
            val decoded = decodeSingle(remaining)
            frames += decoded
            index += when (decoded) {
                is DecodedPassiveFrame.Valid.Request -> decoded.raw.size
                is DecodedPassiveFrame.Valid.Response -> decoded.raw.size
                is DecodedPassiveFrame.Valid.ExceptionResponse -> decoded.raw.size
                is DecodedPassiveFrame.Valid.Generic -> decoded.raw.size
                is DecodedPassiveFrame.InvalidCrc -> remaining.size
                is DecodedPassiveFrame.Truncated -> remaining.size
            }
        }

        return frames
    }

    private fun decodeSingle(data: ByteArray): DecodedPassiveFrame {
        if (data.size < 4) {
            return DecodedPassiveFrame.Truncated(raw = data, slaveId = data.firstOrNull()?.toInt()?.and(0xFF))
        }

        val slaveId = data[0].toInt() and 0xFF
        val functionCode = data[1].toInt() and 0xFF

        if (functionCode and 0x80 != 0 && data.size >= 5) {
            val candidate = data.copyOfRange(0, 5)
            if (ModbusCrc.isValid(candidate)) {
                val baseFunction = functionCode and 0x7F
                val exceptionCode = candidate[2].toInt() and 0xFF
                return DecodedPassiveFrame.Valid.ExceptionResponse(
                    raw = candidate,
                    slaveId = slaveId,
                    functionCode = baseFunction,
                    exceptionCode = exceptionCode
                )
            }
        }

        if (functionCode in setOf(0x03, 0x04)) {
            if (data.size >= 8) {
                val requestCandidate = data.copyOfRange(0, 8)
                if (ModbusCrc.isValid(requestCandidate)) {
                    val startAddress = ((requestCandidate[2].toInt() and 0xFF) shl 8) or (requestCandidate[3].toInt() and 0xFF)
                    val quantity = ((requestCandidate[4].toInt() and 0xFF) shl 8) or (requestCandidate[5].toInt() and 0xFF)
                    return DecodedPassiveFrame.Valid.Request(
                        raw = requestCandidate,
                        slaveId = slaveId,
                        functionCode = functionCode,
                        startAddress = startAddress,
                        quantity = quantity
                    )
                }
            }

            val byteCount = data[2].toInt() and 0xFF
            val responseLength = byteCount + 5
            if (data.size >= responseLength) {
                val responseCandidate = data.copyOfRange(0, responseLength)
                if (ModbusCrc.isValid(responseCandidate)) {
                    return DecodedPassiveFrame.Valid.Response(
                        raw = responseCandidate,
                        slaveId = slaveId,
                        functionCode = functionCode,
                        byteCount = byteCount,
                        responseDataHex = ModbusFrameParser.toHexString(
                            responseCandidate.copyOfRange(3, 3 + byteCount)
                        )
                    )
                }
            } else {
                return DecodedPassiveFrame.Truncated(raw = data, slaveId = slaveId)
            }
        }

        if (data.size >= 8) {
            val fixedLengthCandidate = data.copyOfRange(0, 8)
            if (ModbusCrc.isValid(fixedLengthCandidate)) {
                return DecodedPassiveFrame.Valid.Generic(
                    raw = fixedLengthCandidate,
                    slaveId = slaveId,
                    functionCode = functionCode
                )
            }
        }

        if (ModbusCrc.isValid(data)) {
            return DecodedPassiveFrame.Valid.Generic(
                raw = data,
                slaveId = slaveId,
                functionCode = functionCode
            )
        }

        return DecodedPassiveFrame.InvalidCrc(raw = data, slaveId = slaveId)
    }
}

private sealed interface DecodedPassiveFrame {
    data class InvalidCrc(
        val raw: ByteArray,
        val slaveId: Int?
    ) : DecodedPassiveFrame

    data class Truncated(
        val raw: ByteArray,
        val slaveId: Int?
    ) : DecodedPassiveFrame

    sealed interface Valid : DecodedPassiveFrame {
        val raw: ByteArray
        val slaveId: Int
        val functionCode: Int

        data class Request(
            override val raw: ByteArray,
            override val slaveId: Int,
            override val functionCode: Int,
            val startAddress: Int,
            val quantity: Int
        ) : Valid

        data class Response(
            override val raw: ByteArray,
            override val slaveId: Int,
            override val functionCode: Int,
            val byteCount: Int,
            val responseDataHex: String
        ) : Valid

        data class ExceptionResponse(
            override val raw: ByteArray,
            override val slaveId: Int,
            override val functionCode: Int,
            val exceptionCode: Int
        ) : Valid {
            val exceptionName: String
                get() = when (exceptionCode) {
                    0x01 -> "Illegal Function"
                    0x02 -> "Illegal Data Address"
                    0x03 -> "Illegal Data Value"
                    0x04 -> "Slave Device Failure"
                    else -> "Code %02X".format(exceptionCode)
                }

            val exceptionCodeHex: String
                get() = "%02X".format(exceptionCode)
        }

        data class Generic(
            override val raw: ByteArray,
            override val slaveId: Int,
            override val functionCode: Int
        ) : Valid
    }
}

private data class PendingRequest(
    val slaveId: Int,
    val functionCode: Int,
    val description: String,
    val requestedAt: Long
)

private data class MutableSlaveStats(
    val slaveId: Int,
    var status: SlaveStatus = if (slaveId == 0) SlaveStatus.BROADCAST else SlaveStatus.UNKNOWN,
    var lastSeenTimestamp: Long? = null,
    var lastRequestTimestamp: Long? = null,
    var lastResponseTimestamp: Long? = null,
    var successCount: Int = 0,
    var timeoutCount: Int = 0,
    var crcErrorCount: Int = 0,
    var exceptionCount: Int = 0,
    var lastRequestSummary: String? = null,
    var lastRequestHex: String? = null,
    var lastResponseSummary: String? = null,
    var lastResponseHex: String? = null
) {
    fun toSnapshot(): SlaveCommunicationSummary {
        return SlaveCommunicationSummary(
            slaveId = slaveId,
            status = status,
            lastSeenTimestamp = lastSeenTimestamp,
            successCount = successCount,
            timeoutCount = timeoutCount,
            crcErrorCount = crcErrorCount,
            exceptionCount = exceptionCount,
            lastRequestSummary = lastRequestSummary,
            lastRequestHex = lastRequestHex,
            lastResponseSummary = lastResponseSummary,
            lastResponseHex = lastResponseHex
        )
    }
}

data class CommunicationAnalysisSnapshot(
    val capturedAtTimestamp: Long,
    val totalFrames: Int,
    val crcErrorCount: Int,
    val truncatedFrameCount: Int,
    val timeoutCount: Int,
    val minResponseTimeMs: Long?,
    val avgResponseTimeMs: Int?,
    val maxResponseTimeMs: Long?,
    val intervalJitter: IntervalJitter,
    val slaveSummaries: List<SlaveCommunicationSummary>,
    val abnormalEvents: List<CommunicationAbnormalEvent>
)

data class SlaveCommunicationSummary(
    val slaveId: Int,
    val status: SlaveStatus,
    val lastSeenTimestamp: Long?,
    val successCount: Int,
    val timeoutCount: Int,
    val crcErrorCount: Int,
    val exceptionCount: Int,
    val lastRequestSummary: String?,
    val lastRequestHex: String?,
    val lastResponseSummary: String?,
    val lastResponseHex: String?
)

data class CommunicationAbnormalEvent(
    val timestamp: Long,
    val slaveId: Int?,
    val type: AbnormalType,
    val message: String,
    val hex: String?
)

enum class SlaveStatus {
    UNKNOWN,
    ACTIVE,
    NORMAL,
    NO_RESPONSE,
    WARNING,
    EXCEPTION,
    BROADCAST
}

enum class AbnormalType {
    TIMEOUT,
    CRC_ERROR,
    EXCEPTION,
    TRUNCATED,
    UNEXPECTED_LENGTH
}

enum class IntervalJitter {
    UNKNOWN,
    SMALL,
    MEDIUM,
    LARGE
}

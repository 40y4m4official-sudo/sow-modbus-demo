package com.example.meterdemo.analysis

import com.example.meterdemo.meter.model.SerialParity
import com.example.meterdemo.modbus.ModbusCrc

class SerialSettingsAutoDetector(
    val candidate: SerialSettingsCandidate
) {
    private var validFrameCount = 0
    private var requestCount = 0
    private var responseCount = 0
    private var exceptionCount = 0
    private var genericCount = 0
    private var knownFunctionCount = 0
    private var knownSlaveCount = 0
    private var crcErrorCount = 0
    private var truncatedFrameCount = 0

    @Synchronized
    fun onRawBytes(data: ByteArray) {
        PassiveDetectorDecoder.decode(data).forEach { frame ->
            when (frame) {
                is PassiveDetectorFrame.Truncated -> truncatedFrameCount += 1
                is PassiveDetectorFrame.InvalidCrc -> crcErrorCount += 1
                is PassiveDetectorFrame.Valid.Request -> {
                    validFrameCount += 1
                    requestCount += 1
                    recordValidFrame(frame.slaveId, frame.functionCode)
                }

                is PassiveDetectorFrame.Valid.Response -> {
                    validFrameCount += 1
                    responseCount += 1
                    recordValidFrame(frame.slaveId, frame.functionCode)
                }

                is PassiveDetectorFrame.Valid.ExceptionResponse -> {
                    validFrameCount += 1
                    exceptionCount += 1
                    recordValidFrame(frame.slaveId, frame.functionCode)
                }

                is PassiveDetectorFrame.Valid.Generic -> {
                    validFrameCount += 1
                    genericCount += 1
                    recordValidFrame(frame.slaveId, frame.functionCode)
                }
            }
        }
    }

    @Synchronized
    fun snapshot(): SerialSettingsDetectionResult {
        val score = (requestCount * 18) +
            (responseCount * 18) +
            (exceptionCount * 14) +
            (genericCount * 8) +
            (knownFunctionCount * 3) +
            (knownSlaveCount * 2) -
            (crcErrorCount * 12) -
            (truncatedFrameCount * 8)

        return SerialSettingsDetectionResult(
            candidate = candidate,
            score = score,
            validFrameCount = validFrameCount,
            crcErrorCount = crcErrorCount,
            truncatedFrameCount = truncatedFrameCount,
            confidence = when {
                validFrameCount >= 3 && score >= 80 -> DetectionConfidence.HIGH
                validFrameCount >= 1 && score >= 24 -> DetectionConfidence.MEDIUM
                else -> DetectionConfidence.LOW
            }
        )
    }

    private fun recordValidFrame(slaveId: Int, functionCode: Int) {
        if (slaveId in 0..247) {
            knownSlaveCount += 1
        }
        if (functionCode in KNOWN_FUNCTION_CODES) {
            knownFunctionCount += 1
        }
    }

    private companion object {
        val KNOWN_FUNCTION_CODES = setOf(0x03, 0x04, 0x06, 0x10, 0x2B)
    }
}

data class SerialSettingsCandidate(
    val baudRate: Int,
    val parity: SerialParity,
    val stopBits: Int
) {
    val label: String
        get() = "$baudRate 8${parity.label.first()}$stopBits"
}

data class SerialSettingsDetectionResult(
    val candidate: SerialSettingsCandidate,
    val score: Int,
    val validFrameCount: Int,
    val crcErrorCount: Int,
    val truncatedFrameCount: Int,
    val confidence: DetectionConfidence
)

enum class DetectionConfidence {
    LOW,
    MEDIUM,
    HIGH
}

private object PassiveDetectorDecoder {
    fun decode(data: ByteArray): List<PassiveDetectorFrame> {
        if (data.isEmpty()) return emptyList()

        val frames = mutableListOf<PassiveDetectorFrame>()
        var index = 0
        while (index < data.size) {
            val remaining = data.copyOfRange(index, data.size)
            val decoded = decodeSingle(remaining)
            frames += decoded
            index += when (decoded) {
                is PassiveDetectorFrame.Valid.Request -> decoded.raw.size
                is PassiveDetectorFrame.Valid.Response -> decoded.raw.size
                is PassiveDetectorFrame.Valid.ExceptionResponse -> decoded.raw.size
                is PassiveDetectorFrame.Valid.Generic -> decoded.raw.size
                is PassiveDetectorFrame.InvalidCrc -> remaining.size
                is PassiveDetectorFrame.Truncated -> remaining.size
            }
        }
        return frames
    }

    private fun decodeSingle(data: ByteArray): PassiveDetectorFrame {
        if (data.size < 4) {
            return PassiveDetectorFrame.Truncated
        }

        val slaveId = data[0].toInt() and 0xFF
        val functionCode = data[1].toInt() and 0xFF

        if (functionCode and 0x80 != 0 && data.size >= 5) {
            val candidate = data.copyOfRange(0, 5)
            if (ModbusCrc.isValid(candidate)) {
                return PassiveDetectorFrame.Valid.ExceptionResponse(candidate, slaveId, functionCode and 0x7F)
            }
        }

        if (functionCode in setOf(0x03, 0x04)) {
            if (data.size >= 8) {
                val requestCandidate = data.copyOfRange(0, 8)
                if (ModbusCrc.isValid(requestCandidate)) {
                    return PassiveDetectorFrame.Valid.Request(requestCandidate, slaveId, functionCode)
                }
            }

            val byteCount = data[2].toInt() and 0xFF
            val responseLength = byteCount + 5
            if (data.size >= responseLength) {
                val responseCandidate = data.copyOfRange(0, responseLength)
                if (ModbusCrc.isValid(responseCandidate)) {
                    return PassiveDetectorFrame.Valid.Response(responseCandidate, slaveId, functionCode)
                }
            } else {
                return PassiveDetectorFrame.Truncated
            }
        }

        if (data.size >= 8) {
            val fixedLengthCandidate = data.copyOfRange(0, 8)
            if (ModbusCrc.isValid(fixedLengthCandidate)) {
                return PassiveDetectorFrame.Valid.Generic(fixedLengthCandidate, slaveId, functionCode)
            }
        }

        return if (ModbusCrc.isValid(data)) {
            PassiveDetectorFrame.Valid.Generic(data, slaveId, functionCode)
        } else {
            PassiveDetectorFrame.InvalidCrc
        }
    }
}

private sealed interface PassiveDetectorFrame {
    data object InvalidCrc : PassiveDetectorFrame

    data object Truncated : PassiveDetectorFrame

    sealed interface Valid : PassiveDetectorFrame {
        val raw: ByteArray
        val slaveId: Int
        val functionCode: Int

        data class Request(
            override val raw: ByteArray,
            override val slaveId: Int,
            override val functionCode: Int
        ) : Valid

        data class Response(
            override val raw: ByteArray,
            override val slaveId: Int,
            override val functionCode: Int
        ) : Valid

        data class ExceptionResponse(
            override val raw: ByteArray,
            override val slaveId: Int,
            override val functionCode: Int
        ) : Valid

        data class Generic(
            override val raw: ByteArray,
            override val slaveId: Int,
            override val functionCode: Int
        ) : Valid
    }
}

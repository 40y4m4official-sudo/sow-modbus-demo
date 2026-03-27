package com.example.meterdemo.usb

import com.example.meterdemo.modbus.ModbusCrc
import com.example.meterdemo.modbus.ModbusFrameParser

class UsbRequestFrameAssembler(
    private val frameSize: Int = 8
) {
    private val buffer = mutableListOf<Byte>()

    fun append(
        data: ByteArray,
        expectedSlaveId: Int,
        allowedFunctionCodes: Set<Int>
    ): FrameAssemblyResult {
        buffer.addAll(data.toList())

        val droppedNoise = mutableListOf<Byte>()
        val frames = mutableListOf<ByteArray>()

        while (buffer.size >= frameSize) {
            val matchIndex = findFirstValidFrameStart(expectedSlaveId, allowedFunctionCodes)
            if (matchIndex >= 0) {
                if (matchIndex > 0) {
                    droppedNoise += buffer.take(matchIndex)
                    buffer.subList(0, matchIndex).clear()
                }

                frames += buffer.take(frameSize).toByteArray()
                buffer.subList(0, frameSize).clear()
                continue
            }

            val preserveFrom = findPreserveStart(expectedSlaveId)
            if (preserveFrom > 0) {
                droppedNoise += buffer.take(preserveFrom)
                buffer.subList(0, preserveFrom).clear()
            } else if (preserveFrom < 0) {
                val keepCount = minOf(frameSize - 1, buffer.size)
                val dropCount = buffer.size - keepCount
                if (dropCount > 0) {
                    droppedNoise += buffer.take(dropCount)
                    buffer.subList(0, dropCount).clear()
                }
            } else {
                break
            }
        }

        return FrameAssemblyResult(
            frames = frames,
            droppedNoise = droppedNoise.toByteArray()
        )
    }

    fun clear() {
        buffer.clear()
    }

    private fun findFirstValidFrameStart(
        expectedSlaveId: Int,
        allowedFunctionCodes: Set<Int>
    ): Int {
        for (start in 0..buffer.size - frameSize) {
            val frame = buffer.subList(start, start + frameSize).toByteArray()
            val slaveId = frame[0].toInt() and 0xFF
            val functionCode = frame[1].toInt() and 0xFF
            if (slaveId != expectedSlaveId) continue
            if (functionCode !in allowedFunctionCodes) continue
            if (!ModbusCrc.isValid(frame)) continue
            if (ModbusFrameParser.parseReadHoldingRegistersRequest(frame) == null) continue
            return start
        }
        return -1
    }

    private fun findPreserveStart(expectedSlaveId: Int): Int {
        val tailStart = (buffer.size - (frameSize - 1)).coerceAtLeast(0)
        for (index in tailStart until buffer.size) {
            val slaveId = buffer[index].toInt() and 0xFF
            if (slaveId == expectedSlaveId) {
                return index
            }
        }
        return -1
    }
}

data class FrameAssemblyResult(
    val frames: List<ByteArray>,
    val droppedNoise: ByteArray
)

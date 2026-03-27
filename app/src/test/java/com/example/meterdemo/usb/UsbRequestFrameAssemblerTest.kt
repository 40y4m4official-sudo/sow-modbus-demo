package com.example.meterdemo.usb

import com.example.meterdemo.modbus.ModbusCrc
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbRequestFrameAssemblerTest {

    @Test
    fun append_splitRequestAcrossChunks_reassemblesSingleFrame() {
        val assembler = UsbRequestFrameAssembler()
        val frame = requestFrame(2, 0x03, 37113, 4)

        val first = assembler.append(
            data = frame.copyOfRange(0, 4),
            expectedSlaveId = 2,
            allowedFunctionCodes = setOf(0x03, 0x04)
        )
        val second = assembler.append(
            data = frame.copyOfRange(4, frame.size),
            expectedSlaveId = 2,
            allowedFunctionCodes = setOf(0x03, 0x04)
        )

        assertTrue(first.frames.isEmpty())
        assertEquals(1, second.frames.size)
        assertArrayEquals(frame, second.frames.single())
    }

    @Test
    fun append_noiseBeforeValidFrame_dropsNoiseAndFindsFrame() {
        val assembler = UsbRequestFrameAssembler()
        val frame = requestFrame(2, 0x03, 37113, 4)
        val result = assembler.append(
            data = byteArrayOf(0x0B) + frame,
            expectedSlaveId = 2,
            allowedFunctionCodes = setOf(0x03, 0x04)
        )

        assertEquals(1, result.frames.size)
        assertArrayEquals(frame, result.frames.single())
        assertArrayEquals(byteArrayOf(0x0B), result.droppedNoise)
    }

    @Test
    fun append_partialNoiseAndChunks_resynchronizesOnValidFrame() {
        val assembler = UsbRequestFrameAssembler()
        val frame = requestFrame(2, 0x03, 37113, 4)

        val first = assembler.append(
            data = byteArrayOf(0x0B, frame[0], frame[1], frame[2], frame[3], frame[4], frame[5], frame[6]),
            expectedSlaveId = 2,
            allowedFunctionCodes = setOf(0x03, 0x04)
        )
        val second = assembler.append(
            data = byteArrayOf(frame[7]),
            expectedSlaveId = 2,
            allowedFunctionCodes = setOf(0x03, 0x04)
        )

        assertTrue(first.frames.isEmpty())
        assertArrayEquals(byteArrayOf(0x0B), second.droppedNoise)
        assertEquals(1, second.frames.size)
        assertArrayEquals(frame, second.frames.single())
    }

    private fun requestFrame(
        slaveId: Int,
        functionCode: Int,
        startAddress: Int,
        quantity: Int
    ): ByteArray {
        val payload = byteArrayOf(
            slaveId.toByte(),
            functionCode.toByte(),
            ((startAddress shr 8) and 0xFF).toByte(),
            (startAddress and 0xFF).toByte(),
            ((quantity shr 8) and 0xFF).toByte(),
            (quantity and 0xFF).toByte()
        )
        return ModbusCrc.appendCrc(payload)
    }
}

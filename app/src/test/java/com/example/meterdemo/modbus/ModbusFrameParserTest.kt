package com.example.meterdemo.modbus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModbusFrameParserTest {

    @Test
    fun parseReadHoldingRegistersRequest_validFrame_parsesCorrectly() {
        val frame = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x03,
                0x03,
                0x00,
                0x00,
                0x01
            )
        )

        val request = ModbusFrameParser.parseReadHoldingRegistersRequest(frame)

        requireNotNull(request)
        assertEquals(2, request.slaveId)
        assertEquals(0x03, request.functionCode)
        assertEquals(768, request.startAddress)
        assertEquals(1, request.quantity)
    }

    @Test
    fun parseReadHoldingRegistersRequest_invalidCrc_returnsNull() {
        val frame = byteArrayOf(
            0x02,
            0x03,
            0x03,
            0x00,
            0x00,
            0x01,
            0x00,
            0x00
        )

        val request = ModbusFrameParser.parseReadHoldingRegistersRequest(frame)

        assertNull(request)
    }

    @Test
    fun parseReadHoldingRegistersRequest_tooShort_returnsNull() {
        val frame = byteArrayOf(
            0x02,
            0x03,
            0x03
        )

        val request = ModbusFrameParser.parseReadHoldingRegistersRequest(frame)

        assertNull(request)
    }

    @Test
    fun toHexString_formatsUppercaseHex() {
        val bytes = byteArrayOf(
            0x02,
            0x03,
            0x02,
            0x00,
            0x0F
        )

        val actual = ModbusFrameParser.toHexString(bytes)

        assertEquals("02 03 02 00 0F", actual)
    }
}

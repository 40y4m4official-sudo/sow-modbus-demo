package com.example.meterdemo.modbus

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModbusCrcTest {

    @Test
    fun calculate_knownRequest_returnsExpectedCrc() {
        val requestWithoutCrc = byteArrayOf(
            0x02,
            0x03,
            0x03,
            0x00,
            0x00,
            0x01
        )

        val crc = ModbusCrc.calculate(requestWithoutCrc)

        assertEquals(0xF844, crc)
    }

    @Test
    fun appendCrc_appendsLowThenHigh() {
        val requestWithoutCrc = byteArrayOf(
            0x02,
            0x03,
            0x03,
            0x00,
            0x00,
            0x01
        )

        val actual = ModbusCrc.appendCrc(requestWithoutCrc)

        val expected = byteArrayOf(
            0x02,
            0x03,
            0x03,
            0x00,
            0x00,
            0x01,
            0x44,
            0xF8.toByte()
        )

        assertArrayEquals(expected, actual)
    }

    @Test
    fun isValid_validFrame_returnsTrue() {
        val frame = byteArrayOf(
            0x02,
            0x03,
            0x03,
            0x00,
            0x00,
            0x01,
            0x44,
            0xF8.toByte()
        )

        assertTrue(ModbusCrc.isValid(frame))
    }

    @Test
    fun isValid_invalidFrame_returnsFalse() {
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

        assertFalse(ModbusCrc.isValid(frame))
    }

    @Test
    fun isValid_tooShortFrame_returnsFalse() {
        val frame = byteArrayOf(0x02, 0x03, 0x00)

        assertFalse(ModbusCrc.isValid(frame))
    }
}

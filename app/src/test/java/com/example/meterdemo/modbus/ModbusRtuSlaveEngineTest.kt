package com.example.meterdemo.modbus

import com.example.meterdemo.meter.profiles.MeterProfiles
import com.example.meterdemo.meter.repository.MeterRepository
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ModbusRtuSlaveEngineTest {

    private lateinit var repository: MeterRepository
    private lateinit var engine: ModbusRtuSlaveEngine

    @Before
    fun setUp() {
        repository = MeterRepository(MeterProfiles.default())
        engine = ModbusRtuSlaveEngine(repository)
    }

    @Test
    fun handleRequest_readAddress768_returnsCurrentValue() {
        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 768,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x03,
                0x02,
                0x00,
                0x0F
            )
        )

        assertArrayEquals(expected, response)
    }

    @Test
    fun handleRequest_readAddress778_returnsCurrentValue() {
        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 778,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x03,
                0x02,
                0x00,
                0xD2.toByte()
            )
        )

        assertArrayEquals(expected, response)
    }

    @Test
    fun handleRequest_readAddress1304_returnsCurrentValue() {
        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 1304,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x03,
                0x02,
                0x30,
                0x39
            )
        )

        assertArrayEquals(expected, response)
    }

    @Test
    fun handleRequest_afterValueUpdate_returnsUpdatedValue() {
        repository.setRawValue(768, 22)

        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 768,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x03,
                0x02,
                0x00,
                0x16
            )
        )

        assertArrayEquals(expected, response)
    }

    @Test
    fun handleRequest_unknownAddress_returnsIllegalDataAddress() {
        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 999,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x83.toByte(),
                0x02
            )
        )

        assertArrayEquals(expected, response)
    }

    @Test
    fun handleRequest_quantityNotOne_returnsIllegalDataValue() {
        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 768,
            quantity = 2
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x83.toByte(),
                0x03
            )
        )

        assertArrayEquals(expected, response)
    }

    @Test
    fun handleRequest_unsupportedFunction_returnsIllegalFunction() {
        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x04,
            startAddress = 768,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x84.toByte(),
                0x01
            )
        )

        assertArrayEquals(expected, response)
    }

    @Test
    fun handleRequest_differentSlaveId_returnsNull() {
        val request = requestFrame(
            slaveId = 3,
            functionCode = 0x03,
            startAddress = 768,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        assertNull(response)
    }

    @Test
    fun handleRequest_invalidCrc_returnsNull() {
        val request = byteArrayOf(
            0x02,
            0x03,
            0x03,
            0x00,
            0x00,
            0x01,
            0x00,
            0x00
        )

        val response = engine.handleRequest(request)

        assertNull(response)
    }

    @Test
    fun handleRequest_tooShort_returnsNull() {
        val request = byteArrayOf(
            0x02,
            0x03,
            0x00
        )

        val response = engine.handleRequest(request)

        assertNull(response)
    }

    @Test
    fun handleRequest_responseHasValidCrc() {
        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 768,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        assertNotNull(response)
        assertEquals(true, ModbusCrc.isValid(response!!))
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

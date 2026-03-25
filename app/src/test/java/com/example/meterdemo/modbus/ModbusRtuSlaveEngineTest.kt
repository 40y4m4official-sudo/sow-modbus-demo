package com.example.meterdemo.modbus

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.model.WordByteOrder
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
    fun handleRequest_multipleSequentialRegisters_returnsCombinedPayload() {
        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 768,
            quantity = 3
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x03,
                0x06,
                0x00,
                0x0F,
                0x00,
                0x10,
                0x00,
                0x0E
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

    @Test
    fun handleRequest_function04Profile_acceptsInputRegisterRead() {
        val profile = MeterProfile(
            modelId = "input-only",
            displayName = "Input Register Meter",
            slaveId = 2,
            baudRate = 19200,
            dataBits = 8,
            parity = 2,
            stopBits = 1,
            functionCode = 0x04,
            points = listOf(
                MeterPoint(
                    name = "Input Energy",
                    address = 100,
                    registerCount = 1,
                    gain = 1,
                    dataType = DataType.UINT16,
                    unit = "kWh",
                    initialRawValue = 0x1234
                )
            )
        )
        repository = MeterRepository(profile)
        engine = ModbusRtuSlaveEngine(repository)

        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x04,
            startAddress = 100,
            quantity = 1
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x04,
                0x02,
                0x12,
                0x34
            )
        )

        assertArrayEquals(expected, response)
    }

    @Test
    fun handleRequest_twoWordPoint_honorsConfiguredWordByteOrder() {
        val profile = MeterProfile(
            modelId = "ordered-32bit",
            displayName = "Ordered 32bit Meter",
            slaveId = 2,
            baudRate = 19200,
            dataBits = 8,
            parity = 2,
            stopBits = 1,
            functionCode = 0x03,
            points = listOf(
                MeterPoint(
                    name = "Demand",
                    address = 200,
                    registerCount = 2,
                    gain = 1,
                    dataType = DataType.INT32,
                    wordByteOrder = WordByteOrder.MSB_LSB,
                    unit = "W",
                    initialRawValue = 0x11223344
                )
            )
        )
        repository = MeterRepository(profile)
        engine = ModbusRtuSlaveEngine(repository)

        val request = requestFrame(
            slaveId = 2,
            functionCode = 0x03,
            startAddress = 200,
            quantity = 2
        )

        val response = engine.handleRequest(request)

        requireNotNull(response)

        val expected = ModbusCrc.appendCrc(
            byteArrayOf(
                0x02,
                0x03,
                0x04,
                0x22,
                0x11,
                0x44,
                0x33
            )
        )

        assertArrayEquals(expected, response)
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

package com.example.meterdemo.meter.repository

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.profiles.MeterProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MeterRepositoryTest {

    private lateinit var repository: MeterRepository

    @Before
    fun setUp() {
        repository = MeterRepository(MeterProfiles.default())
    }

    @Test
    fun getProfile_returnsDefaultProfile() {
        val profile = repository.getProfile()

        assertEquals("backup-ct", profile.modelId)
        assertEquals(2, profile.slaveId)
        assertEquals(0x03, profile.functionCode)
    }

    @Test
    fun hasAddress_existingAddress_returnsTrue() {
        assertTrue(repository.hasAddress(768))
        assertTrue(repository.hasAddress(1304))
    }

    @Test
    fun hasAddress_unknownAddress_returnsFalse() {
        assertFalse(repository.hasAddress(999))
    }

    @Test
    fun getRawValue_existingAddress_returnsInitialValue() {
        assertEquals(15, repository.getRawValue(768))
        assertEquals(210, repository.getRawValue(778))
        assertEquals(12345, repository.getRawValue(1304))
    }

    @Test
    fun setRawValue_existingAddress_updatesValue() {
        val updated = repository.setRawValue(768, 22)

        assertTrue(updated)
        assertEquals(22, repository.getRawValue(768))
    }

    @Test
    fun setRawValue_unknownAddress_returnsFalse() {
        val updated = repository.setRawValue(999, 22)

        assertFalse(updated)
    }

    @Test
    fun getFormattedValue_gain1_returnsRawWithUnit() {
        val actual = repository.getFormattedValue(778)

        assertEquals("210 V", actual)
    }

    @Test
    fun snapshot_exposesDecimalGain() {
        val snapshot = repository.snapshot().first { it.address == 1304 }

        assertEquals(100.0, snapshot.gain, 0.0)
    }

    @Test
    fun getFormattedValue_float32Point_decodesBitsAsFloat() {
        repository = MeterRepository(
            MeterProfile(
                modelId = "float-meter",
                displayName = "Float Meter",
                slaveId = 2,
                baudRate = 19200,
                dataBits = 8,
                parity = 2,
                stopBits = 1,
                functionCode = 0x03,
                points = listOf(
                    MeterPoint(
                        name = "Power Factor",
                        address = 1000,
                        registerCount = 2,
                        gain = 1.0,
                        dataType = DataType.FLOAT,
                        unit = "",
                        initialRawValue = 1.25f.toRawBits()
                    )
                )
            )
        )

        assertEquals("1.25", repository.getFormattedValue(1000))
    }

    @Test
    fun getFormattedValue_gain100_returnsScaledValueWithUnit() {
        val actual = repository.getFormattedValue(1304)

        assertEquals("123.45 kWh", actual)
    }

    @Test
    fun snapshot_returnsAllPoints() {
        val snapshots = repository.snapshot()

        assertEquals(9, snapshots.size)
        assertNotNull(snapshots.firstOrNull { it.address == 768 })
        assertNotNull(snapshots.firstOrNull { it.address == 1306 })
    }

    @Test
    fun findPointsForRead_contiguousAddresses_returnsAllRequestedPoints() {
        val points = repository.findPointsForRead(startAddress = 768, quantity = 3)

        assertNotNull(points)
        assertEquals(listOf(768, 769, 770), points!!.map { it.address })
    }

    @Test
    fun resetCurrentProfileValues_restoresInitialValues() {
        repository.setRawValue(768, 99)
        repository.setRawValue(1304, 500)

        repository.resetCurrentProfileValues()

        assertEquals(15, repository.getRawValue(768))
        assertEquals(12345, repository.getRawValue(1304))
    }
}

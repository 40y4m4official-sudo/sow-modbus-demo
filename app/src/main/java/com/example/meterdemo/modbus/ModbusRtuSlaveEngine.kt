package com.example.meterdemo.modbus

import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.WordByteOrder
import com.example.meterdemo.meter.repository.MeterRepository

class ModbusRtuSlaveEngine(
    private val meterRepository: MeterRepository
) {

    fun handleRequest(frame: ByteArray): ByteArray? {
        if (frame.size < 8) return null
        if (!ModbusCrc.isValid(frame)) return null

        val request = ModbusFrameParser.parseReadHoldingRegistersRequest(frame) ?: return null
        val expectedSlaveId = meterRepository.getSlaveId()

        if (request.slaveId != expectedSlaveId) {
            return null
        }

        if (request.functionCode != meterRepository.getFunctionCode()) {
            return buildExceptionResponse(
                slaveId = request.slaveId,
                functionCode = request.functionCode,
                exceptionCode = ModbusExceptionCode.ILLEGAL_FUNCTION
            )
        }

        if (request.quantity <= 0) {
            return buildExceptionResponse(
                slaveId = request.slaveId,
                functionCode = request.functionCode,
                exceptionCode = ModbusExceptionCode.ILLEGAL_DATA_VALUE
            )
        }

        val dataBytes = buildResponseData(request.startAddress, request.quantity)
            ?: return buildExceptionResponse(
                slaveId = request.slaveId,
                functionCode = request.functionCode,
                exceptionCode = ModbusExceptionCode.ILLEGAL_DATA_ADDRESS
            )

        return buildReadRegistersResponse(
            slaveId = request.slaveId,
            functionCode = request.functionCode,
            dataBytes = dataBytes
        )
    }

    private fun buildReadRegistersResponse(
        slaveId: Int,
        functionCode: Int,
        dataBytes: ByteArray
    ): ByteArray {
        val payload = byteArrayOf(
            slaveId.toByte(),
            functionCode.toByte(),
            dataBytes.size.toByte()
        ) + dataBytes

        return ModbusCrc.appendCrc(payload)
    }

    private fun encodePointValue(
        point: MeterPoint,
        rawValue: Int
    ): ByteArray {
        val registerCount = point.registerCount.coerceIn(1, 4)
        val totalBytes = registerCount * 2
        val extensionByte = when (point.dataType) {
            DataType.INT -> if (rawValue < 0) 0xFF.toByte() else 0x00
            DataType.FLOAT -> 0x00
        }

        val naturalBytes = ByteArray(totalBytes) { extensionByte }
        val rawBytes = byteArrayOf(
            ((rawValue ushr 24) and 0xFF).toByte(),
            ((rawValue ushr 16) and 0xFF).toByte(),
            ((rawValue ushr 8) and 0xFF).toByte(),
            (rawValue and 0xFF).toByte()
        )

        val copyLength = minOf(rawBytes.size, naturalBytes.size)
        val srcStart = rawBytes.size - copyLength
        val dstStart = naturalBytes.size - copyLength
        rawBytes.copyInto(
            destination = naturalBytes,
            destinationOffset = dstStart,
            startIndex = srcStart,
            endIndex = rawBytes.size
        )

        val words = naturalBytes
            .toList()
            .chunked(2)
            .map { word -> word.toByteArray() }
            .toMutableList()

        if (point.wordByteOrder == WordByteOrder.LSB_LSB || point.wordByteOrder == WordByteOrder.LSB_MSB) {
            words.reverse()
        }

        if (point.wordByteOrder == WordByteOrder.MSB_LSB || point.wordByteOrder == WordByteOrder.LSB_LSB) {
            for (index in words.indices) {
                words[index] = words[index].reversedArray()
            }
        }

        return words.flatMap { it.asList() }.toByteArray()
    }

    private fun buildResponseData(
        startAddress: Int,
        quantity: Int
    ): ByteArray? {
        val bytes = mutableListOf<Byte>()
        var consumedRegisters = 0
        var currentAddress = startAddress

        while (consumedRegisters < quantity) {
            val point = meterRepository.findPoint(currentAddress)
            if (point != null) {
                if (consumedRegisters + point.registerCount > quantity) {
                    return null
                }
                bytes += encodePointValue(point, meterRepository.requireRawValue(point.address)).toList()
                consumedRegisters += point.registerCount
                currentAddress += point.registerCount
                continue
            }

            if (!meterRepository.isWithinActiveAddressRange(currentAddress)) {
                return null
            }

            bytes += listOf(0x00.toByte(), 0x00.toByte())
            consumedRegisters += 1
            currentAddress += 1
        }

        return bytes.toByteArray()
    }

    private fun buildExceptionResponse(
        slaveId: Int,
        functionCode: Int,
        exceptionCode: Int
    ): ByteArray {
        val payload = byteArrayOf(
            slaveId.toByte(),
            (functionCode or 0x80).toByte(),
            exceptionCode.toByte()
        )

        return ModbusCrc.appendCrc(payload)
    }
}

object ModbusExceptionCode {
    const val ILLEGAL_FUNCTION = 0x01
    const val ILLEGAL_DATA_ADDRESS = 0x02
    const val ILLEGAL_DATA_VALUE = 0x03
}

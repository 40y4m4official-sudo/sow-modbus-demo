package com.example.meterdemo.modbus

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

        val points = meterRepository.findPointsForRead(request.startAddress, request.quantity)
            ?: return buildExceptionResponse(
                slaveId = request.slaveId,
                functionCode = request.functionCode,
                exceptionCode = ModbusExceptionCode.ILLEGAL_DATA_ADDRESS
            )

        val dataBytes = buildList<Byte> {
            points.forEach { point ->
                addAll(encodePointValue(point, meterRepository.requireRawValue(point.address)).toList())
            }
        }.toByteArray()

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
        return when (point.registerCount) {
            1 -> {
                val msb = ((rawValue shr 8) and 0xFF).toByte()
                val lsb = (rawValue and 0xFF).toByte()
                when (point.wordByteOrder) {
                    WordByteOrder.MSB_MSB,
                    WordByteOrder.LSB_MSB -> byteArrayOf(msb, lsb)

                    WordByteOrder.LSB_LSB,
                    WordByteOrder.MSB_LSB -> byteArrayOf(lsb, msb)
                }
            }

            2 -> {
                val b3 = ((rawValue ushr 24) and 0xFF).toByte()
                val b2 = ((rawValue ushr 16) and 0xFF).toByte()
                val b1 = ((rawValue ushr 8) and 0xFF).toByte()
                val b0 = (rawValue and 0xFF).toByte()
                when (point.wordByteOrder) {
                    WordByteOrder.MSB_MSB -> byteArrayOf(b3, b2, b1, b0)
                    WordByteOrder.LSB_LSB -> byteArrayOf(b0, b1, b2, b3)
                    WordByteOrder.MSB_LSB -> byteArrayOf(b2, b3, b0, b1)
                    WordByteOrder.LSB_MSB -> byteArrayOf(b1, b0, b3, b2)
                }
            }

            else -> ByteArray(point.registerCount * 2)
        }
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

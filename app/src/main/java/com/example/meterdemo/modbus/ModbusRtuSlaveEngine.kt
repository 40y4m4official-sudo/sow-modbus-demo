package com.example.meterdemo.modbus

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

        if (request.quantity != 1) {
            return buildExceptionResponse(
                slaveId = request.slaveId,
                functionCode = request.functionCode,
                exceptionCode = ModbusExceptionCode.ILLEGAL_DATA_VALUE
            )
        }

        val point = meterRepository.findPoint(request.startAddress)
            ?: return buildExceptionResponse(
                slaveId = request.slaveId,
                functionCode = request.functionCode,
                exceptionCode = ModbusExceptionCode.ILLEGAL_DATA_ADDRESS
            )

        val rawValue = meterRepository.requireRawValue(point.address)

        return buildReadHoldingRegistersResponse(
            slaveId = request.slaveId,
            functionCode = request.functionCode,
            rawValue = rawValue
        )
    }

    private fun buildReadHoldingRegistersResponse(
        slaveId: Int,
        functionCode: Int,
        rawValue: Int
    ): ByteArray {
        val value16 = rawValue and 0xFFFF

        val payload = byteArrayOf(
            slaveId.toByte(),
            functionCode.toByte(),
            0x02, // 1レジスタ = 2バイト
            ((value16 shr 8) and 0xFF).toByte(), // MSB
            (value16 and 0xFF).toByte()          // LSB
        )

        return ModbusCrc.appendCrc(payload)
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
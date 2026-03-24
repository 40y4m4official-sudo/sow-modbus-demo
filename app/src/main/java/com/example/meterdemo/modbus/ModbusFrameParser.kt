package com.example.meterdemo.modbus

data class ReadHoldingRegistersRequest(
    val slaveId: Int,
    val functionCode: Int,
    val startAddress: Int,
    val quantity: Int
)

object ModbusFrameParser {

    private const val MIN_REQUEST_LENGTH = 8

    fun parseReadHoldingRegistersRequest(frame: ByteArray): ReadHoldingRegistersRequest? {
        if (frame.size < MIN_REQUEST_LENGTH) return null
        if (!ModbusCrc.isValid(frame)) return null

        val slaveId = frame[0].toInt() and 0xFF
        val functionCode = frame[1].toInt() and 0xFF
        val startAddress = ((frame[2].toInt() and 0xFF) shl 8) or (frame[3].toInt() and 0xFF)
        val quantity = ((frame[4].toInt() and 0xFF) shl 8) or (frame[5].toInt() and 0xFF)

        return ReadHoldingRegistersRequest(
            slaveId = slaveId,
            functionCode = functionCode,
            startAddress = startAddress,
            quantity = quantity
        )
    }

    fun toHexString(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
    }
}
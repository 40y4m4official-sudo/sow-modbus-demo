package com.example.meterdemo.modbus

object ModbusCrc {

    fun calculate(data: ByteArray, length: Int = data.size): Int {
        var crc = 0xFFFF

        for (i in 0 until length) {
            crc = crc xor (data[i].toInt() and 0xFF)

            repeat(8) {
                crc = if ((crc and 0x0001) != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }

        return crc and 0xFFFF
    }

    fun isValid(frame: ByteArray): Boolean {
        if (frame.size < 4) return false

        val bodyLength = frame.size - 2
        val calculated = calculate(frame, bodyLength)
        val received = ((frame[bodyLength + 1].toInt() and 0xFF) shl 8) or
                (frame[bodyLength].toInt() and 0xFF)

        return calculated == received
    }

    fun appendCrc(dataWithoutCrc: ByteArray): ByteArray {
        val crc = calculate(dataWithoutCrc)
        val result = ByteArray(dataWithoutCrc.size + 2)

        System.arraycopy(dataWithoutCrc, 0, result, 0, dataWithoutCrc.size)
        result[result.lastIndex - 1] = (crc and 0xFF).toByte()         // CRC Lo
        result[result.lastIndex] = ((crc shr 8) and 0xFF).toByte()     // CRC Hi

        return result
    }
}
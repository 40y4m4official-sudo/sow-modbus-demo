package com.example.meterdemo.viewmodel

import androidx.lifecycle.ViewModel
import com.example.meterdemo.logging.CommLog
import com.example.meterdemo.logging.CommLogger
import com.example.meterdemo.meter.profiles.MeterProfiles
import com.example.meterdemo.meter.repository.MeterRepository
import com.example.meterdemo.meter.repository.MeterValueSnapshot
import com.example.meterdemo.modbus.ModbusCrc
import com.example.meterdemo.modbus.ModbusFrameParser
import com.example.meterdemo.modbus.ModbusRtuSlaveEngine
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private val repository = MeterRepository(MeterProfiles.default())
    private val engine = ModbusRtuSlaveEngine(repository)
    private val logger = CommLogger()

    val logs: StateFlow<List<CommLog>> = logger.logs

    fun getProfileName(): String = repository.getProfile().displayName

    fun getSnapshots(): List<MeterValueSnapshot> = repository.snapshot()

    fun updateRawValue(address: Int, text: String) {
        val value = text.toIntOrNull()
        if (value == null) {
            logger.error("数値に変換できません: address=$address, input=$text")
            return
        }

        val updated = repository.setRawValue(address, value)
        if (!updated) {
            logger.error("未対応のアドレスです: $address")
            return
        }

        logger.info("値を更新しました: address=$address, value=$value")
    }

    fun resetValues() {
        repository.resetCurrentProfileValues()
        logger.info("全レジスタを初期値に戻しました")
    }

    fun clearLogs() {
        logger.clear()
        logger.info("ログをクリアしました")
    }

    fun simulateRead(address: Int) {
        val requestWithoutCrc = byteArrayOf(
            repository.getSlaveId().toByte(),
            repository.getFunctionCode().toByte(),
            ((address shr 8) and 0xFF).toByte(),
            (address and 0xFF).toByte(),
            0x00,
            0x01
        )

        val request = ModbusCrc.appendCrc(requestWithoutCrc)
        logger.rx(ModbusFrameParser.toHexString(request), "読取要求 addr=$address")

        val response = engine.handleRequest(request)
        if (response == null) {
            logger.error("応答を生成できませんでした")
            return
        }

        logger.tx(ModbusFrameParser.toHexString(response), "読取応答")
    }

    fun simulateCustomRequest(hexText: String) {
        val frame = parseHexString(hexText)
        if (frame == null) {
            logger.error("HEX文字列の解析に失敗しました")
            return
        }

        logger.rx(ModbusFrameParser.toHexString(frame), "カスタム要求")

        val response = engine.handleRequest(frame)
        if (response == null) {
            logger.error("応答を生成できませんでした")
            return
        }

        logger.tx(ModbusFrameParser.toHexString(response), "カスタム応答")
    }

    private fun parseHexString(input: String): ByteArray? {
        val cleaned = input
            .replace("\n", " ")
            .replace("\t", " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }

        return try {
            ByteArray(cleaned.size) { index ->
                cleaned[index].toInt(16).toByte()
            }
        } catch (_: Exception) {
            null
        }
    }
}

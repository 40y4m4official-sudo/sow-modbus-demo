package com.example.meterdemo.usb

import android.content.Context
import android.hardware.usb.UsbManager

data class UsbDeviceSummary(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val productName: String?,
    val manufacturerName: String?
) {
    val displayLabel: String
        get() = buildString {
            append(productName ?: manufacturerName ?: deviceName.substringAfterLast('/'))
            append(" (VID:PID ")
            append(vendorId.toString(16).padStart(4, '0').uppercase())
            append(":")
            append(productId.toString(16).padStart(4, '0').uppercase())
            append(")")
        }
}

class UsbDeviceScanner(
    context: Context
) {
    private val usbManager = context.getSystemService(UsbManager::class.java)

    fun scan(): List<UsbDeviceSummary> {
        val manager = usbManager ?: return emptyList()
        return manager.deviceList.values
            .sortedBy { it.deviceName }
            .map { device ->
                UsbDeviceSummary(
                    deviceName = device.deviceName,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    productName = device.productName,
                    manufacturerName = device.manufacturerName
                )
            }
    }
}

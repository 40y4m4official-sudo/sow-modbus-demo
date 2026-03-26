package com.example.meterdemo.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager

class UsbSerialConnectionManager(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onPermissionResult(deviceName: String, granted: Boolean)
        fun onConnected(deviceName: String)
        fun onDisconnected(deviceName: String?, reason: String)
        fun onDataReceived(data: ByteArray)
        fun onError(message: String)
    }

    private val usbManager = context.getSystemService(UsbManager::class.java)
    private val prober = UsbSerialProber.getDefaultProber()
    private val permissionIntent = PendingIntent.getBroadcast(
        context,
        1001,
        Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    private var receiverRegistered = false
    private var currentDeviceName: String? = null
    private var currentPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_USB_PERMISSION) return
            val device = extractUsbDevice(intent) ?: return
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            listener.onPermissionResult(device.deviceName, granted)
        }
    }

    init {
        registerReceiver()
    }

    fun requestPermission(deviceName: String): Boolean {
        val manager = usbManager ?: return false
        val device = manager.deviceList.values.firstOrNull { it.deviceName == deviceName } ?: return false
        manager.requestPermission(device, permissionIntent)
        return true
    }

    fun connect(deviceName: String): Boolean {
        val manager = usbManager ?: return false
        val driver = findDriver(deviceName) ?: return false.also {
            listener.onError("USB serial device not found: $deviceName")
        }
        val device = driver.device
        if (!manager.hasPermission(device)) {
            listener.onError("USB permission required for ${device.deviceName}")
            return false
        }

        disconnect("Reconnect")

        return try {
            val connection = manager.openDevice(device)
                ?: return false.also { listener.onError("Failed to open USB device") }
            val port = driver.ports.firstOrNull()
                ?: return false.also { listener.onError("No serial port found on USB device") }
            port.open(connection)
            port.setParameters(
                19200,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_EVEN
            )
            port.dtr = true
            port.rts = true

            currentDeviceName = device.deviceName
            currentPort = port
            ioManager = SerialInputOutputManager(
                port,
                object : SerialInputOutputManager.Listener {
                    override fun onNewData(data: ByteArray) {
                        listener.onDataReceived(data)
                    }

                    override fun onRunError(e: Exception) {
                        listener.onError(e.message ?: "USB serial I/O error")
                        disconnect("I/O stopped")
                    }
                }
            ).also { managerInstance ->
                managerInstance.start()
            }

            listener.onConnected(device.deviceName)
            true
        } catch (e: Exception) {
            listener.onError("USB connect failed: ${e.message ?: "unknown error"}")
            disconnect("Connect failed")
            false
        }
    }

    fun disconnect(reason: String = "Disconnected") {
        ioManager?.stop()
        ioManager = null
        runCatching { currentPort?.close() }
        if (currentDeviceName != null) {
            listener.onDisconnected(currentDeviceName, reason)
        }
        currentPort = null
        currentDeviceName = null
    }

    fun write(data: ByteArray): Boolean {
        val port = currentPort ?: return false
        return try {
            port.write(data, 2000)
            true
        } catch (e: Exception) {
            listener.onError("USB write failed: ${e.message ?: "unknown error"}")
            false
        }
    }

    fun release() {
        disconnect("Released")
        unregisterReceiver()
    }

    fun isConnected(): Boolean = currentPort != null

    fun connectedDeviceName(): String? = currentDeviceName

    private fun findDriver(deviceName: String): UsbSerialDriver? {
        val manager = usbManager ?: return null
        return prober.findAllDrivers(manager).firstOrNull { it.device.deviceName == deviceName }
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            permissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    private fun unregisterReceiver() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(permissionReceiver) }
        receiverRegistered = false
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.meterdemo.USB_PERMISSION"
    }

    @Suppress("DEPRECATION")
    private fun extractUsbDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }
}

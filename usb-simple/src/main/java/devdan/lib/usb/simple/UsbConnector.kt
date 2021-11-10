package devdan.lib.usb.simple

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbConnector private constructor(private val context: Context) {

    companion object {
        private var instance: UsbConnector? = null

        @Synchronized
        fun getInstance(context: Context): UsbConnector {
            if (instance == null) {
                instance = UsbConnector(context)
            }

            return instance as UsbConnector
        }

        const val TAG = "USB"
    }

    private val _usbManager: UsbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    private var _connectListeners: ArrayList<OnUsbConnectListener> = arrayListOf()

    var driver: IDriver? = null
        private set

    var device: UsbDevice? = null
        private set

    var actionPermission: String = UsbCommon.ACTION_USB_PERMISSION

    fun addOnUsbConnectListener(listener: OnUsbConnectListener) {
        _connectListeners.add(listener)
    }

    fun removeOnUsbConnectListener(listener: OnUsbConnectListener) {
        _connectListeners.remove(listener)
    }

    fun findAndConnectDevice(): Boolean {
        val device = _usbManager.deviceList.values.find {
            it.productId == AlcodiProtocol.PRODUCT_ID
        }

        return if (device != null) {
            connectDevice(device)
            true
        } else {
            false
        }
    }

    fun connectDevice(device: UsbDevice) {
        if (device.productId != AlcodiProtocol.PRODUCT_ID || isOpened()) {
            return
        }

        if (_usbManager.hasPermission(device)) {
            openDevice(device)
        } else {
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(actionPermission),
                0
            ).also { intent ->
                _usbManager.requestPermission(device, intent)
            }
        }
    }

    fun openDevice(device: UsbDevice) {
        this.device = device

        val connection = _usbManager.openDevice(device)
        if (connection != null) {
            if (device.productId == AlcodiDriver.PRODUCT_ID) {
                driver = AlcodiDriver(context, device).also {
                    it.open(connection)
                    it.getDriverInfo()
                    _connectListeners.forEach { listener ->
                        listener.onConnect(driver = it)
                    }
                }
            }
        }
    }

    fun stopDevice() {
        driver?.close()
        driver = null
    }

    @Synchronized
    fun closeDevice() {
        driver?.close()
        driver = null
        device = null

        _connectListeners.forEach { it.onDisconnect() }
    }

    @Synchronized
    fun release() {

        driver?.close()
        driver = null
        device = null

        _connectListeners.clear()
        instance = null
    }

    fun isOpened(): Boolean = driver != null

    fun getDeviceMap(): HashMap<String, UsbDevice> = _usbManager.deviceList

    fun isConnect(): Boolean {
        return _usbManager.deviceList.values.find {
            it.productId == AlcodiProtocol.PRODUCT_ID
        } != null
    }
}

interface OnUsbConnectListener {
    fun onConnect(driver: IDriver)
    fun onDisconnect()
    fun onConnectError()
}
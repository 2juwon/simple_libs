package devdan.lib.usb.simple

import android.hardware.usb.UsbDeviceConnection

/**
 * Driver Interface
 */
interface IDriver {

    /**
     * open device
     */
    fun open(usbDeviceConnection: UsbDeviceConnection)

    /**
     * write data. android -> device
     */
    suspend fun write(buffer: ByteArray): Int

    /**
     * start // stop
     */
    fun start()

    fun stop()

    // close
    fun close()

    fun getProductId(): Int

    fun getVendorId(): Int
}
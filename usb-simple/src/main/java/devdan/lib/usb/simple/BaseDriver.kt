package devdan.lib.usb.simple

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import kotlinx.coroutines.*
import java.io.IOException

abstract class BaseDriver<REC_DATA>(private val usbDevice: UsbDevice) : IDriver {
    var readEndPoint: UsbEndpoint? = null
    var writeEndPoint: UsbEndpoint? = null

    var connection: UsbDeviceConnection? = null
    var usbInterface: UsbInterface? = null

    protected val scope = CoroutineScope(Dispatchers.IO + Job())

    fun writeAsync(buffer: ByteArray) {
        scope.launch {
            write(buffer)
        }
    }

    override fun open(usbDeviceConnection: UsbDeviceConnection) {
        this.connection = usbDeviceConnection
        for (i in 0 until usbDevice.interfaceCount) {
            val usbIface = usbDevice.getInterface(i)
            if (!connection!!.claimInterface(usbIface, true)) {
                throw IOException("Could not claim data interface")
            }
        }

        this.usbInterface = usbDevice.getInterface(usbDevice.interfaceCount - 1)
        openInterface(usbInterface!!)
    }

    override fun close() {
        stop()

        connection?.let {
            it.releaseInterface(usbInterface)
            it.close()
        }
    }

    abstract fun openInterface(usbInterface: UsbInterface)
}

data class DeviceOutput<T>(val output: T? = null, val isCompleted: Boolean = false)

abstract class ReadBaseTaskFactory<T>(
    protected val driver: BaseDriver<T>,
    protected val readByteSize: Int = 4096,
    protected val timeout: Int = 0,
    val listener: ReadListener<T>?
) {
    private var _isRunning: Boolean = false

    private lateinit var _job: Job

    private val syncObject = Object()

    fun start() {
        stop()

        _isRunning = true

        _job = GlobalScope.launch(Dispatchers.IO) {
            while (isActive && _isRunning) {
                synchronized(syncObject) {
                    val buffer = ByteArray(readByteSize)
                    val bytes = driver.connection!!.bulkTransfer(
                        driver.readEndPoint,
                        buffer, readByteSize,
                        timeout
                    )

                    if (bytes == readByteSize) {
                        parsingData(buffer)?.let {
                            it.output?.let { data -> listener?.onNewData(data) }
                            if (it.isCompleted) {
                                listener?.onComplete()
                            }
                        }
                    }
                }
            }
        }
        _job.start()
    }

    fun stop() {
        _isRunning = false

        if (::_job.isInitialized && _job.isActive) {
            _job.cancel()
        }
    }

    abstract fun parsingData(buffer: ByteArray): DeviceOutput<T>?
}
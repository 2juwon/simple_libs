package devdan.lib.usb.simple

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import kotlinx.coroutines.*

abstract class BaseDriver<REC_DATA>(private val context: Context, private val usbDevice: UsbDevice) : IDriver {
    var readEndPoint: UsbEndpoint? = null
    var writeEndPoint: UsbEndpoint? = null

    var connection: UsbDeviceConnection? = null
    var usbInterface: UsbInterface? = null

    protected var readListener: ReadListener<REC_DATA>? = null
        private set

    protected val scope = CoroutineScope(Dispatchers.IO + Job())

    fun setReadListener(listener: ReadListener<REC_DATA>) {
        this.readListener = listener
    }

    fun removeReadListener() {
        readListener = null
    }

    fun writeAsync(buffer: ByteArray) {
        scope.launch {
            val result = write(buffer)
            showLog(context, "USB", "WRITE : ${buffer.size} / ${buffer[0]} / $result")
        }
    }

    override fun open(usbDeviceConnection: UsbDeviceConnection): Boolean {
        this.connection = usbDeviceConnection
        for (i in 0 until usbDevice.interfaceCount) {
            val usbIface = usbDevice.getInterface(i)
            if (!connection!!.claimInterface(usbIface, true)) {
                return false
            }
        }

        this.usbInterface = usbDevice.getInterface(usbDevice.interfaceCount - 1)
        return if (usbInterface != null) {
            openInterface(usbInterface!!)
            true
        } else {
            false
        }
    }

    override fun close() {
        stop()

        connection?.let {
            usbInterface?.let { usb -> it.releaseInterface(usb) }
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

    @DelicateCoroutinesApi
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
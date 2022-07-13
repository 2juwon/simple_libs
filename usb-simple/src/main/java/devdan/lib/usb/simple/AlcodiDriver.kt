package devdan.lib.usb.simple

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AlcodiDriver(val context: Context, val device: UsbDevice) :
    BaseDriver<AlcodiData>(context, device) {

    companion object {
        const val PRODUCT_ID = AlcodiProtocol.PRODUCT_ID
    }

    private var factory: ReadTaskFactory? = null

    val alcodiDeviceInfo: AlcodiDeviceInfo = AlcodiDeviceInfo()
    private lateinit var stateCheckTimer: Timer

    override fun openInterface(usbInterface: UsbInterface) {
        for (i in 0 until usbInterface.endpointCount) {
            val ep = usbInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                if (ep.direction == UsbConstants.USB_DIR_IN) {
                    readEndPoint = ep
                } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                    writeEndPoint = ep
                }
            }
        }
    }

    override suspend fun write(buffer: ByteArray): Int = suspendCoroutine { cont ->
        connection?.let {
//            val bytes = it.controlTransfer(
//                0x21, 0x09, 0x03, 0x00, buffer, buffer.size,
//                AlcodiProtocol.SEND_TIMEOUT
//            )
            val bytes = it.bulkTransfer(
                writeEndPoint,
                buffer,
                buffer.size,
                AlcodiProtocol.SEND_TIMEOUT
            )
            cont.resume(bytes)
        }
    }

    override fun start() {
        writeAsync(AlcodiProtocol.REQUEST_START)
        startSendTask()
        startRead()
    }

    override fun stop() {
        writeAsync(AlcodiProtocol.REQUEST_CANCEL)
        stopRead()
        stopSendTask()
    }

    override fun getProductId(): Int = device.productId

    override fun getVendorId(): Int = device.vendorId

    fun getDriverInfo() {
        GlobalScope.launch(Dispatchers.IO) {
            connection?.let {
                val request = AlcodiProtocol.REQUEST_STATUS
//                it.controlTransfer(
//                    0x21, 0x09, 0x03, 0x00, request, request.size,
//                    AlcodiProtocol.SEND_TIMEOUT
//                )
                it.bulkTransfer(
                    writeEndPoint,
                    request,
                    request.size,
                    AlcodiProtocol.SEND_TIMEOUT
                )

                val buffer = ByteArray(AlcodiProtocol.RECEIVE_BYTE_SIZE)
                val readBytes = it.bulkTransfer(readEndPoint, buffer, buffer.size, 0)
                if (readBytes == AlcodiProtocol.RECEIVE_BYTE_SIZE) {
                    val serialLow = buffer[AlcodiProtocol.INDEX_SERIAL_LOW]
                    val serialMiddle = buffer[AlcodiProtocol.INDEX_SERIAL_MIDDLE]
                    val serialProd = buffer[AlcodiProtocol.INDEX_SERIAL_HIGH]
                    val serialVendor = buffer[AlcodiProtocol.INDEX_SERIAL_VENDOR]

                    val countryCode = AlcodiProtocol.fetchCountryCode(
                        buffer[AlcodiProtocol.INDEX_COUNTRY_CODE_LOW],
                        buffer[AlcodiProtocol.INDEX_COUNTRY_CODE_HIGH]
                    )

                    val modelNumber = buffer[AlcodiProtocol.INDEX_MODEL_NUMBER].toPositiveInt()
                    val productCode = buffer[AlcodiProtocol.INDEX_PRODUCT_CODE].toChar()
                    val serialNumber = AlcodiProtocol.fetchSerialNumber(
                        serialLow,
                        serialMiddle
                    )
                    val productionNumber = AlcodiProtocol.fetchProductionNumber(serialProd)
                    val vendorYear = AlcodiProtocol.fetchVendorYear(serialVendor)
                    //A 01 KR193200274
                    val serial =
                        "$productCode$modelNumber$countryCode$vendorYear$productionNumber$serialNumber"


                    val releaseDate = AlcodiProtocol.fetchDate(
                        buffer[AlcodiProtocol.INDEX_RELEASE_YEAR],
                        buffer[AlcodiProtocol.INDEX_RELEASE_MONTH],
                        buffer[AlcodiProtocol.INDEX_RELEASE_DAY]
                    )
                    val correctionDate = AlcodiProtocol.fetchDate(
                        buffer[AlcodiProtocol.INDEX_CORRECTION_YEAR],
                        buffer[AlcodiProtocol.INDEX_CORRECTION_MONTH],
                        buffer[AlcodiProtocol.INDEX_CORRECTION_DAY]
                    )

                    val useCount = AlcodiProtocol.fetchUseCount(
                        buffer[AlcodiProtocol.INDEX_USE_COUNT_LOW],
                        buffer[AlcodiProtocol.INDEX_USE_COUNT_HIGH]
                    )

                    with(alcodiDeviceInfo) {
                        this.vendorId = getVendorId()
                        this.productId = getProductId()
                        this.serialNumber = serial
                        this.productCode = productCode.toString()
                        this.countryCode = countryCode
                        this.modelNumber = modelNumber
                        this.releaseDate = releaseDate
                        this.correctionDate = correctionDate
                        this.useCount = useCount
                    }
                }
            }
        }
    }

    @DelicateCoroutinesApi
    private fun startRead() {
        factory = ReadTaskFactory().also { it.start() }
    }

    private fun stopRead() {
        factory?.stop()
        removeReadListener()
    }

    private fun startSendTask() {
        stopSendTask()

        stateCheckTimer = Timer()
        stateCheckTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                writeAsync(AlcodiProtocol.REQUEST_STATUS)
            }
        }, 0L, AlcodiProtocol.SEND_INTERVAL)
    }

    private fun stopSendTask() {
        if (::stateCheckTimer.isInitialized) {
            stateCheckTimer.cancel()
        }
    }

    inner class ReadTaskFactory :
        ReadBaseTaskFactory<AlcodiData>(
            this,
            AlcodiProtocol.RECEIVE_BYTE_SIZE,
            AlcodiProtocol.READ_TIMEOUT,
            readListener
        ) {
        private var deviceState: Byte? = null
            get() = if (field == null) AlcodiProtocol.STATE_CODE_WAIT else field

        override fun parsingData(buffer: ByteArray): DeviceOutput<AlcodiData>? {
            val stateCode = buffer[AlcodiProtocol.INDEX_STATE_CODE]

            if (stateCode != deviceState) {
                if (deviceState == AlcodiProtocol.STATE_CODE_WAIT && stateCode == AlcodiProtocol.STATE_CODE_SHOW_RESULT) {
                    return null
                }

                deviceState = stateCode

                if (deviceState == AlcodiProtocol.STATE_CODE_SHOW_RESULT) {
                    stop()

                    val blown = buffer[AlcodiProtocol.INDEX_BLOWN].toInt()

                    val bac = AlcodiProtocol.fetchResult(
                        buffer[AlcodiProtocol.INDEX_BAC_LOW],
                        buffer[AlcodiProtocol.INDEX_BAC_HIGH]
                    )
                    val useCount = AlcodiProtocol.fetchUseCount(
                        buffer[AlcodiProtocol.INDEX_USE_COUNT_LOW],
                        buffer[AlcodiProtocol.INDEX_USE_COUNT_HIGH]
                    )

                    return DeviceOutput(AlcodiData(stateCode, blown, bac, useCount), true)
                } else {
                    return DeviceOutput(AlcodiData(stateCode))
                }
            }

            return null
        }
    }
}

interface ReadListener<T> {
    fun onNewData(data: T)
    fun onComplete()
    fun onError(err: Throwable)
}

data class AlcodiData(
    val state: Byte,
    val blown: Int = -9999,
    val bac: Double = -9999.0,
    val useCount: Int = -9999
) {
    override fun toString(): String {
        return "state=$state, blown=$blown, bac=$bac, useCount=$useCount"
    }
}
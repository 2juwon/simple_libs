package devdan.simple

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import devdan.lib.usb.simple.*

class MainActivity : AppCompatActivity(), OnUsbConnectListener {

    private val usbConnector: UsbConnector by lazy {
        UsbConnector.getInstance(this)
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

            if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                if (usbConnector.device == null) {
                    updateConnect(false)
                } else if (device != null && usbConnector.device == device) {
                    updateConnect(false)
                    usbConnector.closeDevice()
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                device?.let {
                    updateConnect(true)
                    if (!usbConnector.isOpened()) {
                        usbConnector.findAndConnectDevice()
                    }
                }
            } else if (UsbCommon.ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    device?.let { usbDevice ->
                        intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false).also {
                            if (it) {
                                usbConnector.openDevice(usbDevice)
                            } else {
                                showToast("권한 거부. 다시 실행하세요!!")
                            }
                        }
                    }
                }
            }
        }
    }

    private lateinit var snText: TextView
    private lateinit var blowText: TextView
    private lateinit var stateText: TextView
    private lateinit var bacText: TextView
    private lateinit var connectText: TextView
    private var alcodiDriver: AlcodiDriver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectText = findViewById(R.id.text_connect)
        snText = findViewById(R.id.text_sn)
        blowText = findViewById(R.id.text_blow)
        stateText = findViewById(R.id.text_state)
        bacText = findViewById(R.id.text_bac)

        val filter = IntentFilter()
        filter.addAction(UsbCommon.ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(usbReceiver, filter)

        usbConnector.addOnUsbConnectListener(this)
    }

    private fun updateConnect(isConnect: Boolean) {
        if (isConnect) {
            connectText.text = "CONNECT"
        } else {
            connectText.text = "DISCONNECT"
        }
    }

    private fun initView() {
        snText.text = ""
        blowText.text = ""
        stateText.text = ""
        bacText.text = ""
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onConnect(driver: IDriver) {
        if (driver is AlcodiDriver) {
            alcodiDriver = driver

            if (alcodiDriver != null) {
                snText.text = alcodiDriver!!.alcodiDeviceInfo.serialNumber
                blowText.text = alcodiDriver!!.alcodiDeviceInfo.useCount.toString()

                initUsbDataManager()
            } else {
                showToast("??")
            }
        } else {
            alcodiDriver = null
            showToast("???")
        }
    }

    private fun postUpdateState(data: AlcodiData) {
        when (data.state) {
            AlcodiProtocol.STATE_CODE_READY -> {
                stateText.post {
                    snText.text = alcodiDriver?.alcodiDeviceInfo?.serialNumber
                    blowText.text = alcodiDriver?.alcodiDeviceInfo?.useCount.toString()
                    bacText.text = ""
                    stateText.text = "준비중"
                }

            }
            AlcodiProtocol.STATE_CODE_WAIT_MEASURE -> {
                stateText.post {
                    stateText.text = "불어주세요"
                }
            }
            AlcodiProtocol.STATE_CODE_BLOWN_CHECK -> {
                stateText.post {
                    stateText.text = "측정중"
                }
            }
            AlcodiProtocol.STATE_CODE_ANALYSIS -> {
                stateText.post {
                    stateText.text = "분석중"
                }
            }
            AlcodiProtocol.STATE_CODE_SHOW_RESULT -> {
                stateText.post {
                    stateText.text = "완료!"
                    blowText.text = data.useCount.toString()
                    bacText.text = "${data.bac}%"
                }
            }
        }

    }

    private fun initUsbDataManager() {
        if (alcodiDriver != null) {
            with(alcodiDriver!!) {
                setReadListener(object : ReadListener<AlcodiData> {
                    override fun onNewData(data: AlcodiData) {
                        showLog(application, "AD", data.toString())
                        postUpdateState(data)
                    }

                    override fun onComplete() {
                        stop()
                    }

                    override fun onError(err: Throwable) {
                        showLog(application, "RBTask", "err is $err")
                    }

                })

                start()
                writeAsync(AlcodiProtocol.REQUEST_START)
            }
        }
    }


    override fun onDisconnect() {
        updateConnect(false)
        alcodiDriver?.stop()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (e: IllegalArgumentException) {
        } catch (e: Exception) {
        } finally {
            usbConnector.removeOnUsbConnectListener(this)
            usbConnector.release()
        }
    }
}
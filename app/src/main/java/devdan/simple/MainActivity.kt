package devdan.simple

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import devdan.lib.usb.simple.*
import devdan.libs.base.activity.BaseActivity
import devdan.libs.base.recyclerview.BaseRecyclerViewAdapter
import devdan.libs.base.recyclerview.ViewHolderDataViewBinding
import devdan.simple.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(), OnUsbConnectListener {
    private val _viewModel by viewModels<MainViewModel>()

    private val usbConnector: UsbConnector by lazy {
        UsbConnector.getInstance(this)
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

            if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                if (usbConnector.device == null) {
                    _viewModel.updateConnect(false)
                } else if (device != null && usbConnector.device == device) {
                    _viewModel.updateConnect(false)
                    usbConnector.closeDevice()
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                device?.let {
                    _viewModel.updateConnect(true)
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

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun getVariables(): Map<Int, ViewModel> = mapOf(
        Pair(BR.viewModel, _viewModel)
    )

    override fun initObserver() {
        val filter = IntentFilter()
        filter.addAction(UsbCommon.ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(usbReceiver, filter)

        usbConnector.addOnUsbConnectListener(this)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onConnect(driver: IDriver) {
        if (driver is AlcodiDriver) {
            _viewModel.setUsbDriver(driver)
        } else {
            _viewModel.setUsbDriver(null)
            showToast("???")
        }
    }

    override fun onDisconnect() {
        _viewModel.disconnectUsb()
    }

    override fun onConnectError() {
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
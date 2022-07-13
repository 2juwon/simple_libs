package devdan.libs.ble.classic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import devdan.libs.base.utils.Logger
import devdan.libs.ble.ScanListener

/**
 * Bluetooth classic scanner
 *
 * @param context use register broadcast receiver
 * @param scanListener 스캔해서 찾은 디바이스을 알려주거나, 스캔 종료를 알려준다.
 */
class BluetoothClassicScanner(
    private val context: Context,
    private val scanListener: ScanListener
) {
    private val _bluetoothAdapter: BluetoothAdapter?
        get() = BluetoothAdapter.getDefaultAdapter()

    private var _isScanning = false
    private val _bleDiscoveryBroadcastReceiver: BroadcastReceiver

    companion object {
        const val TAG = "BleScanner"
    }

    init {
        _bleDiscoveryBroadcastReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothDevice.ACTION_FOUND) {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.type != BluetoothDevice.DEVICE_TYPE_DUAL && device?.name?.isNotEmpty() == true) {
                        Logger.e(TAG, device.address + "/" + device.name)
                        scanListener.onFounded(device)
                    }
                } else if (intent.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                    Logger.d(TAG, "discover Finish")
                    scanListener.onDiscoverFinished()
                    stopScan(context)
                }
            }
        }
    }


    private fun registerReceiver() {
        unregisterReceiver()

        val bleDiscoveryIntentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        context.registerReceiver(_bleDiscoveryBroadcastReceiver, bleDiscoveryIntentFilter)
    }

    private fun unregisterReceiver() {
        try {
            context.unregisterReceiver(_bleDiscoveryBroadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(context: Context) {
        if (_bluetoothAdapter != null) {
            _isScanning = true

            registerReceiver()

            _bluetoothAdapter?.startDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan(context: Context) {
        _isScanning = false
        if (_bluetoothAdapter != null) {
            _bluetoothAdapter?.cancelDiscovery()
            unregisterReceiver()
        }
    }

    fun isScanning(): Boolean {
        return _isScanning
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): Set<BluetoothDevice?>? {
        return _bluetoothAdapter?.bondedDevices
    }
}
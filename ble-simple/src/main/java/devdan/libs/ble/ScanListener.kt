package devdan.libs.ble

import android.bluetooth.BluetoothDevice

interface ScanListener {
    fun onFounded(device: BluetoothDevice)
    fun onDiscoverFinished()
    fun onScanFailed()
}
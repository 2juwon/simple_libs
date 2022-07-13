package devdan.libs.ble.classic

import android.bluetooth.BluetoothDevice

interface SocketEvent {
    fun onConnect()
    fun onDisconnect()
    fun onConnectError(device: BluetoothDevice)
}
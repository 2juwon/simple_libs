package devdan.libs.ble.classic

interface SerialListener {
    fun onSerialConnect()
    fun onSerialConnectError(e: Exception)
    fun onSerialDisconnect()
}
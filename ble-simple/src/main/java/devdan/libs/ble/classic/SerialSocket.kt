package devdan.libs.ble.classic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import devdan.libs.base.utils.ConvertUtil
import devdan.libs.base.utils.Logger
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

abstract class SerialSocket(
    private val device: BluetoothDevice,
    protected val socketOption: SocketOption
) : Runnable {
    companion object {
        private val BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    protected var listener: SerialListener? = null
    private var _socket: BluetoothSocket? = null
    private var _isConnected: Boolean = false
    private val _writeThread: WriteThread = WriteThread()
    protected val readBuffers = LinkedList<ByteArray>()

    @Throws(ParsingException::class)
    abstract fun readData(data: ByteArray)

    fun isConnected(): Boolean = _isConnected

    @Throws(IOException::class)
    fun connect(listener: SerialListener) {
        this.listener = listener
        Executors.newSingleThreadExecutor().submit(this)
    }

    fun write(data: ByteArray) {
        _writeThread.queue(data)
    }

    private fun startWriteThread() {
        _writeThread.start()
    }

    fun disconnect() {
        listener = null // ignore remaining data and errors
        _isConnected = false
        closeSocket()
    }

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    private fun openSocket() {
        val uuid = if (device.uuids.isNotEmpty()) device.uuids[0].uuid else BLUETOOTH_SPP
        _socket = device.createRfcommSocketToServiceRecord(uuid)
        _socket?.connect()

        _isConnected = true
        listener?.onSerialConnect()
    }

    private fun closeSocket() {
        _isConnected = false
        try {
            _socket?.close()
            _writeThread.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _socket = null
        }
        listener?.onSerialDisconnect()
    }

    private fun processReadBuffers(data: ByteArray) {
        try {
            if (data[0] == socketOption.startByte) {
                readBuffers.clear()
            }

            readBuffers.add(data)

            if (data[data.size - 1] == socketOption.endByte) {
                var totalLen = 0

                for (buffers in readBuffers) {
                    totalLen += buffers.size
                }

                val totalBuf = ByteArray(totalLen)
                var destPos = 0
                for (buffers in readBuffers) {
                    System.arraycopy(buffers, 0, totalBuf, destPos, buffers.size)
                    destPos += buffers.size
                }

                if (totalLen == socketOption.dataSize) {
                    Logger.d(socketOption.tag, ConvertUtil.prettyPrintByteArray(totalBuf))
                    readData(totalBuf)
                } else {
                    Logger.d(
                        socketOption.tag,
                        "NF : ${ConvertUtil.prettyPrintByteArray(totalBuf)}"
                    )
                    if (totalLen % socketOption.dataSize == 0) {
                        val step = totalLen / socketOption.dataSize
                        for (x in 0 until step) {
                            val startPos = 0 + (socketOption.dataSize * x)
                            val endPos = startPos + socketOption.dataSize
                            val buffer =
                                totalBuf.slice(IntRange(startPos, endPos - 1)).toByteArray()
//                        System.arraycopy(totalBuf, 0 + (DATA_LEN * x), buffer, 0, buffer.size)
                            Logger.d(
                                socketOption.tag,
                                "SPLICE : ${ConvertUtil.prettyPrintByteArray(buffer)}"
                            )
                            readData(buffer)
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
    }

    override fun run() {
        try {
            openSocket()
            startWriteThread()
        } catch (e: Exception) {
            e.printStackTrace()
            closeSocket()
            listener?.onSerialConnectError(e)
            return
        }

        var len: Int
        var data: ByteArray
        val buffer = ByteArray(1024)
        while (true) {
            try {
                len = _socket?.inputStream?.read(buffer) ?: 0
                if (len > 0) {
                    data = buffer.copyOf(len)
                    processReadBuffers(data)
                }
//                var len = _socket?.inputStream?.available()
//                if (len != 0) {
//                    val buffer = ByteArray(1024)
//                    SystemClock.sleep(100);
//                    len = _socket?.inputStream?.available()!!
//                    _socket?.inputStream?.read(buffer, 0, len)
//                    val data = buffer.copyOf(len)
//                    processReadBuffers(data)
//                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isConnected = false
                break
            }
        }

        closeSocket()
    }

    private inner class WriteThread : Thread("BLE write thread") {
        private val writeBuffers: Queue<ByteArray> = LinkedList()
        private var isRunning = false

        fun queue(data: ByteArray) {
            writeBuffers.offer(data)
        }

        override fun run() {
            Logger.e(socketOption.tag, "start WRITE thread")
            isRunning = true
            while (isRunning) {
                try {
                    synchronized(writeBuffers) {
                        writeBuffers.poll()?.let { data ->
                            try {
                                if (!_isConnected) {
                                    throw IOException("not connected")
                                }
                                Logger.e(socketOption.tag, "WRITE : $data")
                                _socket?.outputStream?.write(data)
                            } catch (e: IOException) {
                                e.printStackTrace()
                                closeSocket()
                            }
                        }
                    }

                    sleep(1000)
                } catch (e: Exception) {
                    isRunning = false
                    break
                }
            }
            Logger.e(socketOption.tag, "stop thread")
        }

        fun cancel() {
            isRunning = false
        }
    }
}

data class SocketOption(
    val startByte: Byte,
    val endByte: Byte,
    val dataSize: Int,
    val tag: String
)

class ParsingException : Exception("occured parsing exception")
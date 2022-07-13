package devdan.libs.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid

/**
 * BLE Scanner
 *
 * @param scanListener 스캔해서 찾은 디바이스을 알려주거나, 스캔 종료를 알려준다.
 */
class BleScanner(private val scanListener: ScanListener) {
    private val _bluetoothAdapter: BluetoothAdapter?
        get() = BluetoothAdapter.getDefaultAdapter()

    private var _scanOptions : ScanOptions? = null

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                if(_scanOptions != null && _scanOptions!!.filterUUUID != null) {
                    // 특정 UUID를 찾았으니 스캔을 종료한다.
                    stopScan()
                }
                scanListener.onFounded(scanResult.device)
            }
        }
    }

    /**
     * scan 시작.
     *
     * @param options 스캔 옵션 설정. ScanSettings, filter UUID 설정
     */
    @SuppressLint("MissingPermission")
    fun startTScan(options: ScanOptions) {
        if(_bluetoothAdapter != null && _bluetoothAdapter!!.bluetoothLeScanner != null) {
            _bluetoothAdapter?.run {
                if (options.filterUUUID != null) {
                    for (device in bondedDevices) {
                        if (device.uuids != null && device.uuids.find {
                                it.uuid == options.filterUUUID
                            } != null) {
                            scanListener.onFounded(device)
                            return
                        }
                    }

                    val filterList = mutableListOf<ScanFilter>()
                    filterList.add(
                        ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid(options.filterUUUID))
                            .build()
                    )
                    bluetoothLeScanner.startScan(
                        filterList,
                        options.scanSettings,
                        scanCallback
                    )
                } else {
                    for (device in bondedDevices) {
                        scanListener.onFounded(device)
                    }
                    bluetoothLeScanner.startScan(
                        null,
                        options.scanSettings,
                        scanCallback
                    )
                }
            }
        } else {
            scanListener.onScanFailed()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        _bluetoothAdapter?.takeIf { it.bluetoothLeScanner != null }?.run {
            bluetoothLeScanner.stopScan(scanCallback)
        }
    }
}
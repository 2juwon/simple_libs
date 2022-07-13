package devdan.libs.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import androidx.annotation.NonNull
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.common.callback.ht.TemperatureMeasurementDataCallback
import no.nordicsemi.android.ble.common.profile.ht.TemperatureType
import no.nordicsemi.android.ble.common.profile.ht.TemperatureUnit
import no.nordicsemi.android.ble.observer.BondingObserver
import java.util.*

/**
 * BLE Manager 클래스
 *
 * nordic BLE Library 활용.
 *
 * setConnectionObserver
 * set
 */
class SimpleBleManager(@NonNull context: Context) : BleManager(context) {
    private var _bleReadDataCallback: BleReadDataCallback? = null

    /**
     * Setting read data callback
     *
     * @param dataCallback Ble read data callback
     */
    fun setDataReadCallback(dataCallback: BleReadDataCallback) {
        this._bleReadDataCallback = dataCallback
    }

    /**
     * connect to BLE Device
     * retry 3, delay 100ms
     * set a auto connect false
     * timeout not to set
     */
    fun connectBle(@NonNull device: BluetoothDevice) {
        connect(device)
            .retry(3, 100)
            .useAutoConnect(false)
//            .timeout(2000L)
            .enqueue()
    }

    /**
     * disconnect BLE device
     */
    fun disconnectBle() {
        disconnect().enqueue()
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return object: BleManagerGattCallback() {
            private var htCharacteristic: BluetoothGattCharacteristic? = null

            override fun initialize() {
                super.initialize()
                setIndicationCallback(htCharacteristic)
                    .with(object : TemperatureMeasurementDataCallback() {
                        override fun onTemperatureMeasurementReceived(
                            device: BluetoothDevice,
                            temperature: Float,
                            @TemperatureUnit unit: Int,
                            calendar: Calendar?,
                            @TemperatureType type: Int?
                        ) {
                            _bleReadDataCallback?.onReadData(
                                TemperatureData(
                                    temperature = temperature.toDouble(),
                                    unit = unit,
                                    calendar = calendar,
                                    type = type
                                )
                            )
                        }
                    })
                enableIndications(htCharacteristic).enqueue()
            }
            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                val service = gatt.getService(BleConstants.UUID_BLE_HEALTH_THEMOMETER_SERVICE)
                if (service != null) {
                    htCharacteristic =
                        service.getCharacteristic(BleConstants.UUID_BLE_HEALTH_THEMOMETER_TEMPERATURE_MEASUREMENT_CHARACTERISTIC)
                }
                return htCharacteristic != null
            }

            override fun onServicesInvalidated() {
            }
        }
    }
}

interface BleReadDataCallback {
    fun onReadData(data: TemperatureData)
}
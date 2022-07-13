package devdan.simple

import android.app.Application
import androidx.lifecycle.MutableLiveData
import devdan.lib.usb.simple.*
import devdan.libs.base.viewmodel.BaseViewModel

class MainViewModel(application: Application) : BaseViewModel(application) {
    val connectState: MutableLiveData<String> = MutableLiveData()
    val serialNumber: MutableLiveData<String> = MutableLiveData()
    val blowCount: MutableLiveData<Int> = MutableLiveData()
    val state: MutableLiveData<String> = MutableLiveData()
    val bac: MutableLiveData<Double> = MutableLiveData()

    var usbDriver: AlcodiDriver? = null
        private set

    private fun setSerialNumber(serial: String?) {
        serialNumber.value = serial
    }

    private fun setBlowCount(count: Int?) {
        blowCount.value = count
    }

    private fun setBac(bac: Double?) {
        this.bac.value = bac
    }

    private fun setState(state: String?) {
        this.state.value = state
    }

    private fun init() {
        serialNumber.value = null
        blowCount.value = null
        state.value = null
        bac.value = null
    }

    fun setUsbDriver(driver: AlcodiDriver?) {
        this.usbDriver = driver

        setSerialNumber(driver?.alcodiDeviceInfo?.serialNumber)
        setBlowCount(driver?.alcodiDeviceInfo?.useCount)

        usbDriver?.run {
            setReadListener(object : ReadListener<AlcodiData> {
                override fun onNewData(data: AlcodiData) {
                    showLog(getApplication(), "AD", data.toString())
                    updateUiForState(data)
                }

                override fun onComplete() {
                    stop()
                }

                override fun onError(err: Throwable) {
                    showLog(getApplication(), "RBTask", "err is $err")
                }
            })

            start()
        }
    }

    fun updateConnect(isConnect: Boolean) {
        if (isConnect) {
            connectState.value = "CONNECT"
        } else {
            connectState.value = "DISCONNECT"
        }
    }

    fun updateUiForState(data: AlcodiData) {
        when (data.state) {
            AlcodiProtocol.STATE_CODE_READY -> {
                serialNumber.postValue(usbDriver?.alcodiDeviceInfo?.serialNumber)
                blowCount.postValue(usbDriver?.alcodiDeviceInfo?.useCount)
                bac.postValue(null)
                state.postValue("준비중")
            }
            AlcodiProtocol.STATE_CODE_WAIT_MEASURE -> {
                state.postValue("불어주세요")
            }
            AlcodiProtocol.STATE_CODE_BLOWN_CHECK -> {
                state.postValue("측정중")
            }
            AlcodiProtocol.STATE_CODE_ANALYSIS -> {
                state.postValue("분석중")
            }
            AlcodiProtocol.STATE_CODE_SHOW_RESULT -> {
                state.postValue("완료!")
                blowCount.postValue(data.useCount)
                bac.postValue(data.bac)
            }
        }
    }

    fun disconnectUsb() {
        updateConnect(false)
        usbDriver?.stop()
        init()
    }

    fun onClickStart() {
        usbDriver?.start()
    }

    fun onClickStop() {
        usbDriver?.stop()
    }
}
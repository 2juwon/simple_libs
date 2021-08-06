package devdan.libs.base.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import devdan.libs.base.network.LoadStatus
import devdan.libs.base.widgets.SingleLiveEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers

abstract class BaseViewModel protected constructor(application: Application) :
    AndroidViewModel(application) {

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }

    protected val ioDispatchers = Dispatchers.IO + coroutineExceptionHandler
    protected val uiDispatchers = Dispatchers.Main + coroutineExceptionHandler

    protected val _loadStatus: MutableLiveData<LoadStatus> by lazy { MutableLiveData() }
    val loadStatus: LiveData<LoadStatus>
        get() = _loadStatus

    private val _hideKeyboardEvent: SingleLiveEvent<Unit> by lazy { SingleLiveEvent() }
    val hideKeyboardEvent: LiveData<Unit>
        get() = _hideKeyboardEvent

    fun postLoading() {
        _loadStatus.postValue(LoadStatus.LOADING)
    }

    fun postLoaded() {
        _loadStatus.postValue(LoadStatus.LOADED)
    }

    fun postLoadedEmpty() {
        _loadStatus.postValue(LoadStatus.EMPTY)
    }

    fun postHideKeyboard() {
        _hideKeyboardEvent.postCall()
    }

    fun notifyLoading() {
        _loadStatus.value = LoadStatus.LOADING
    }

    fun notifyLoaded() {
        _loadStatus.value = LoadStatus.LOADED
    }

    fun notifyLoadedEmpty() {
        _loadStatus.value = LoadStatus.EMPTY
    }

    fun hideKeyboard() {
        _hideKeyboardEvent.clickCall()
    }
}
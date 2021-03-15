package devdan.libs.base.utils

import android.util.Log
object Logger {
    const val TAG = "jenda_log"
    fun e(tag: String, msg: String?) {
        Log.e(tag, msg!!)
    }

    fun e(tag: String, msg: String?, exc: Throwable) {
        Log.e(tag, msg!!, exc)
    }

    fun d(tag: String, msg: String?) {
        Log.d(tag, msg!!)
    }

    fun w(tag: String, msg: String?) {
        Log.w(tag, msg!!)
    }
}
package devdan.libs.base.extensions

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel

fun AndroidViewModel.showToast(message: String) {
    showToast(message, Toast.LENGTH_LONG)
}

fun AndroidViewModel.showToast(@StringRes resId: Int) {
    showToast(resId, Toast.LENGTH_LONG)
}

fun AndroidViewModel.showToast(message: String, duration: Int) {
    Toast.makeText(getApplication(), message, duration).show()
}

fun AndroidViewModel.showToast(@StringRes resId: Int, duration: Int) {
    Toast.makeText(getApplication(), resId, duration).show()
}
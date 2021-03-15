package devdan.libs.base.extensions

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment


fun AppCompatActivity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun AppCompatActivity.showToast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

fun AppCompatActivity.showToast(message: String, length: Int) {
    Toast.makeText(this, message, length).show()
}

fun AppCompatActivity.showToast(@StringRes resId: Int, length: Int) {
    Toast.makeText(this, resId, length).show()
}

fun AppCompatActivity.isAddedFragment(tag: String): Boolean =
    supportFragmentManager.findFragmentByTag(tag) != null

fun AppCompatActivity.dismiss(tag: String) {
    supportFragmentManager.findFragmentByTag(tag)?.let {
        if (it is DialogFragment) {
            it.dismiss()
        }
    }
}

fun AppCompatActivity.setStatusWhite() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    } else {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
    window.statusBarColor = Color.WHITE
}

fun AppCompatActivity.hideKeyboard() {
    val context = this
    val imm: InputMethodManager? =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?

    if (imm != null) {
        context.currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}
package devdan.libs.base.extensions

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.DialogInterface
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

fun Fragment.showToast(@StringRes resID: Int) {
    Toast.makeText(requireContext(), resID, Toast.LENGTH_SHORT).show()
}

fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Fragment.createAlert(
    title: String,
    message: String,
    positiveListener: DialogInterface.OnClickListener?
): AlertDialog.Builder {
    return AlertDialog.Builder(requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("확인", positiveListener)
}

fun Fragment.createAlert(
    @StringRes titleResId: Int,
    @StringRes messageResId: Int,
    positiveListener: DialogInterface.OnClickListener?,
    negativeListener: DialogInterface.OnClickListener?
): AlertDialog.Builder {
    return createAlert(
        title = getString(titleResId),
        message = getString(messageResId),
        positiveListener = positiveListener,
        negativeListener = negativeListener
    )
}

fun Fragment.createAlert(
    title: String,
    message: String,
    positiveListener: DialogInterface.OnClickListener?,
    negativeListener: DialogInterface.OnClickListener?
): AlertDialog.Builder {
    val builder = AlertDialog.Builder(requireContext())
        .setTitle(title)
        .setMessage(message)

    if (positiveListener != null)
        builder.setPositiveButton("확인", positiveListener)

    if (negativeListener != null)
        builder.setNegativeButton("취소", negativeListener)

    return builder
}

fun Fragment.hideKeyboard() {
    val context = requireActivity()
    val imm: InputMethodManager? =
        context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?

    if (imm != null) {
        context.currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}

fun DialogFragment.showDialog(fm: FragmentManager, tag: String) {
    try {
        if (fm.findFragmentByTag(tag) == null) {
            fm.beginTransaction()
                .add(this, tag)
                .commitAllowingStateLoss()
        }
    } catch (e: Exception) {

    }
}

fun Fragment.setStatusWhite() {
    requireActivity().window.run {
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        statusBarColor = Color.WHITE
    }
}

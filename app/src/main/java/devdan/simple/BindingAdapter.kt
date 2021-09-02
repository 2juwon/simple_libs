package devdan.simple

import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("bac")
fun setBac(textView: TextView, bac: Double?) {
    if(bac == null) {
        textView.text = ""
    } else {
        textView.text = "${bac}%"
    }
}
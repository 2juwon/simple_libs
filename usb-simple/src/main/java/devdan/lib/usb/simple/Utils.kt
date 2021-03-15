package devdan.lib.usb.simple

import android.content.Context
import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


fun getFileName(): String {
    val timeStamp: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    return "log_$timeStamp.txt"
}

fun showLog(context: Context, tag: String?, text: String?) {
    val logFile = File(
        "${
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            .toString()
        }/${getFileName()}"
    )
    if (!logFile.exists()) {
        try {
            logFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//        Logger.e(tag!!, text)
    try {
        val buf = BufferedWriter(FileWriter(logFile, true))
        buf.append("[").append(tag).append("] ").append(text)
        buf.newLine()
        buf.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}
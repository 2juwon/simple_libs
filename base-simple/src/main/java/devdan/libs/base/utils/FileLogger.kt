package devdan.libs.base.utils

import android.content.Context
import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {
    private fun createLogFileName(): String {
        val timeStamp: String =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return "log_$timeStamp.txt"
    }

    /**
     * init file logger
     *
     * @param context Context
     * @param forceDelete force delete when create a file
     */
    fun initLog(context: Context, forceDelete: Boolean = false) {
        val file = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        file?.let {
            File("$it/${createLogFileName()}").apply {
                if (exists()) {
                    if (forceDelete) {
                        this.deleteRecursively()
                        createNewFile()
                    }
                } else {
                    createNewFile()
                }

                try {
                    val timeStamp: String =
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val buf = BufferedWriter(FileWriter(this, true))
                    buf.append("=============$timeStamp=============")
                    buf.newLine()
                    buf.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Append log. Log example is `[TAG`] Message
     *
     * @param context Context
     * @param tag Log tag
     * @param text Log message
     */
    fun appendLog(context: Context, tag: String?, text: String?) {
        val logFile = File(
            "${
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    .toString()
            }/${createLogFileName()}"
        )
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        Logger.e(tag!!, text)
        try {
            val buf = BufferedWriter(FileWriter(logFile, true))
            buf.append("[").append(tag).append("] ").append(text)
            buf.newLine()
            buf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
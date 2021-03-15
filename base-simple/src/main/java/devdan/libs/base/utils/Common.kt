package devdan.libs.base.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

fun getAppVersion(context: Context?): String {
    var version = ""
    val packageInfo: PackageInfo
    if (context == null) {
        return version
    }
    try {
        packageInfo = context.applicationContext
            .packageManager
            .getPackageInfo(context.applicationContext.packageName, 0)
        version = packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        Logger.e("AppVersion", "getVersionInfo Error :" + e.message)
    }
    return version
}

fun getTemperatureGrade(temperature: Double): TemperatureGrade =
    when (temperature) {
        in 33.0..37.5 -> TemperatureGrade.NORMAL
        37.6 -> TemperatureGrade.MILD
        in 37.7..38.5 -> TemperatureGrade.FEVER
        in 38.6..45.0 -> TemperatureGrade.HIGH
        else -> TemperatureGrade.INCORRECT
    }

enum class TemperatureGrade {
    NORMAL,
    MILD,
    FEVER,
    HIGH,
    INCORRECT
}


fun getFileName(): String {
    val timeStamp: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    return "log_$timeStamp.txt"
}

fun initLog(context: Context, forceDelete: Boolean = false) {
    val file = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    file?.let {
        File("$it/${getFileName()}").apply {
            if (exists()) {
                if (forceDelete) {
                    this.deleteRecursively()
                } else {
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
    }
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

fun goMarket(context: Context) {
    val appPackageName = context.packageName

    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$appPackageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
    } catch (anfe: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
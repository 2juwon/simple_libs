package devdan.libs.base.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri

object ContextUtil {
    /**
     * get a current app version
     * @param context Context
     */
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

    /**
     * start play store market
     *
     * @param context Context
     */
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
}
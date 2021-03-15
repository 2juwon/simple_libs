package devdan.libs.base.widgets

import android.Manifest
import devdan.libs.base.BuildConfig

object AppPermissions {
    const val REQUEST_PERMISSION = 1001

    val requirePermissions: Array<String> =
        if (BuildConfig.DEBUG) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
}
package devdan.libs.ble

import android.bluetooth.le.ScanSettings
import android.os.Build
import java.util.*

data class ScanOptions(
    var scanSettings: ScanSettings = ScanSettings.Builder().apply {
        setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        setReportDelay(0)
    }.build(),
    val filterUUUID: UUID? = null
)

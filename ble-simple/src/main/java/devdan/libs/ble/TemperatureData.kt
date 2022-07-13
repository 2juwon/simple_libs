package devdan.libs.ble

import java.io.Serializable
import java.util.*

data class TemperatureData(
    val temperature: Double,
    val unit: Int?,
    val calendar: Calendar?,
    val type: Int?,
) : Serializable {
    override fun toString(): String {
        return "temperature=$temperature, unit=$unit, calendar=$calendar, type=$type"
    }
}

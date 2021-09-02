package devdan.libs.base.utils

/**
 * get temperature grade using temperature ℃
 *
 * @param temperature ℃
 */
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
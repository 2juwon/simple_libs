package devdan.libs.base.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    const val DEFAULT_FORMAT: String = "yyyy-MM-dd HH:mm:ss"

    fun getDate(type: DateType): DateRange {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val dateRange = DateRange()

        when (type) {
            DateType.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                dateRange.start = format.format(calendar.time)

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                dateRange.end = format.format(calendar.time)
            }
            DateType.WEEKLY -> {
                val today = calendar.get(Calendar.DAY_OF_MONTH)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val startDay = today - (dayOfWeek - 1)

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    startDay
                )
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                dateRange.start = format.format(calendar.time)

                if (startDay <= 0) {
                    calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH))
                }

                calendar.set(Calendar.DAY_OF_MONTH, today + (7 - dayOfWeek))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                dateRange.end = format.format(calendar.time)
            }
            DateType.LAST_WEEKLY -> {
                val today = calendar.get(Calendar.DAY_OF_MONTH)
                val startDay = today - 7

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    startDay
                )
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                dateRange.start = format.format(calendar.time)

                if (startDay <= 0) {
                    calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH))
                }

                calendar.set(Calendar.DAY_OF_MONTH, today)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                dateRange.end = format.format(calendar.time)
            }
            DateType.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                dateRange.start = format.format(calendar.time)

                val gc = GregorianCalendar(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    1
                )
                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    gc.getActualMaximum(Calendar.DAY_OF_MONTH)
                )
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                dateRange.end = format.format(calendar.time)
            }
            else -> {
            }
        }

        return dateRange
    }

    fun getDataRangeStrings(from: Long, to: Long): DateRange {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()

        start.timeInMillis = from
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)

        end.timeInMillis = to
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)

        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return DateRange(parseString(start.time), parseString(end.time))
    }

    fun parseMillisToString(millis: Long, format: String = "yyyy-MM-dd HH:mm:ss"): String {
        val date = Date(millis)
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(date)
    }

    fun parseString(date: Date, format: String = "yyyy-MM-dd HH:mm:ss"): String {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(date)
    }

    fun parseFormat(value: String?, fromFormat: String, toFormat: String): String? {
        val dateFormat = SimpleDateFormat(fromFormat, Locale.getDefault())
        return try {
            val date = dateFormat.parse(value!!)
            if (date != null)
                SimpleDateFormat(toFormat, Locale.getDefault()).format(date)
            else
                null
        } catch (e: Exception) {
            null
        }
    }

    fun parseDate(dateString: String, format: String = "yyyy-MM-dd HH:mm:ss"): Date? {
        return try {
            SimpleDateFormat(format, Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun getNow(format: String = "yyyy-MM-dd HH:mm:ss"): String {
        val date = Date()
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(date)
    }

    fun compareDate(srcDate: String, format: String = "yyyy-MM-dd HH:mm:ss"): Boolean {
        val sf = SimpleDateFormat(format, Locale.getDefault())
        sf.parse(srcDate)?.run {
            val srcCal = Calendar.getInstance()
            srcCal.time = this
            val now = Calendar.getInstance()
            return srcCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                    srcCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        }

        return false
    }
}

data class DateRange(var start: String = "", var end: String = "")

enum class DateType {
    TODAY,
    WEEKLY,
    LAST_WEEKLY,
    MONTHLY,
    ALL,
    SELECT_RANGE
}
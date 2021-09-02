package devdan.libs.base.extensions;

fun Byte.toPositiveInt() = toInt() and 0xFF
infix fun Byte.and(mask: Int): Int = toInt() and mask
infix fun Short.and(mask: Int): Int = toInt() and mask
infix fun Int.and(mask: Long): Long = toLong() and mask
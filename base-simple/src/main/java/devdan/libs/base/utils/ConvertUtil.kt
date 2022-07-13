package devdan.libs.base.utils

object ConvertUtil {
    fun prettyPrintByteArray(byteArray: ByteArray): String {
        return "[${
            byteArray.joinToString(separator = " ") {
                "%02x".format(it)
            }
        }]"
    }
}
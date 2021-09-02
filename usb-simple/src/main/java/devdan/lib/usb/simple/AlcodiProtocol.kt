package devdan.lib.usb.simple

class AlcodiProtocol {
    companion object {
        const val RECEIVE_BYTE_SIZE = 64
        const val SEND_INTERVAL = 300L
        const val SEND_TIMEOUT = 300  // TIMEOUT is substituted 4000 with 500
        const val READ_TIMEOUT = 300  // TIMEOUT is substituted 4000 with 500

        const val VENDOR_ID = 0x04D8
        const val PRODUCT_ID = 0xF5F1

        private val DEFAULT_PACKET: ByteArray = ByteArray(64)

        val REQUEST_STATUS = DEFAULT_PACKET.copyOf().apply { set(0, 0x30) }
        val REQUEST_START = DEFAULT_PACKET.copyOf().apply { set(0, 0x80.toByte()) }
        val REQUEST_CANCEL = DEFAULT_PACKET.copyOf().apply { set(0, 0x81.toByte()) }

        /**
         * 시리얼 번호. Vendor, Hi Byte, Mi Byte, Lo Byte(Hex값 순서대로 나열)
         */
        const val INDEX_SERIAL_LOW = 18
        const val INDEX_SERIAL_MIDDLE = 19
        const val INDEX_SERIAL_HIGH = 20
        const val INDEX_SERIAL_VENDOR = 21

        /**
         * 국가코드 Lo Byte. ASCII
         */
        const val INDEX_COUNTRY_CODE_LOW = 22 // ASCII

        /**
         * 국가코드 Hi Byte. ASCII
         */
        const val INDEX_COUNTRY_CODE_HIGH = 23 // ASCII

        const val INDEX_MODEL_NUMBER = 24

        /**
         * ASCII
         */
        const val INDEX_PRODUCT_CODE = 25 // ASCII

        /**
         * 0x00 => 절전 상태(휴기 상태)
         * 0x01 => 센서 준비 상태
         * 0x02 => 측정 대기 상태
         * 0x03 => 불기 감지 상태
         * 0x04 => 분석 중
         * 0x05 => 계산 중
         * 0x06 => 결과 표시 상태
         * 0x07 => None
         * 0x08 => None
         * 0x09 => None
         */
        const val INDEX_STATE_CODE = 26
        const val INDEX_BLOWN = 27
        const val INDEX_BAC_LOW = 28
        const val INDEX_BAC_HIGH = 29
        const val INDEX_USE_COUNT_LOW = 31
        const val INDEX_USE_COUNT_HIGH = 32
        const val INDEX_RELEASE_YEAR = 38
        const val INDEX_RELEASE_MONTH = 39
        const val INDEX_RELEASE_DAY = 40
        const val INDEX_CORRECTION_YEAR = 41
        const val INDEX_CORRECTION_MONTH = 42
        const val INDEX_CORRECTION_DAY = 43

        const val STATE_CODE_WAIT: Byte = 0x00
        const val STATE_CODE_READY: Byte = 0x01
        const val STATE_CODE_WAIT_MEASURE: Byte = 0x02
        const val STATE_CODE_BLOWN_CHECK: Byte = 0x03
        const val STATE_CODE_ANALYSIS: Byte = 0x04
        const val STATE_CODE_CALCULATING: Byte = 0x05
        const val STATE_CODE_SHOW_RESULT: Byte = 0x06

        /**
         * 결과값 = ((Hi Byte*256)+Lo Byte) / 1000
         */
        fun fetchResult(low: Byte, high: Byte): Double =
            ((high.toPositiveInt() * 256) + low.toPositiveInt()) / 1000.0

        /**
         * (Lo Byte)0x55 => 85, (Hi Byte)0x01 => 256 일때, (1*256)+85 = 00341 일련번호
         */
        fun fetchSerialNumber(low: Byte, middle: Byte): String {
            return String.format("%05d", ((middle.toPositiveInt() * 256) + low.toPositiveInt()))
        }

        fun fetchVendorYear(data: Byte): String {
            return String.format("%02d", data.toPositiveInt())
        }

        fun fetchProductionNumber(high: Byte): String {
            val number = high.toPositiveInt()
            return if(number < 10) {
                String.format("%02d", number)
            } else {
                number.toString()
            }
        }

        /**
         * 국가코드
         */
        fun fetchCountryCode(low: Byte, high: Byte): String {
            return "${high.toPositiveInt().toChar()}${low.toPositiveInt().toChar()}"
        }

        /**
         * 사용횟수 = (Hi Byte*256)+Lo Byte
         */
        fun fetchUseCount(low: Byte, high: Byte): Int =
            ((high.toPositiveInt() * 256) + low.toPositiveInt())

        /**
         * 연도(yy-mm-dd)
         */
        fun fetchDate(year: Byte, month: Byte, day: Byte): String {
            if (year.toInt() == -1 || month.toInt() == -1 || day.toInt() == -1) {
                return "None"
            }
            return String.format("%02d-%02d-%02d", year.toPositiveInt(), month.toPositiveInt(), day.toPositiveInt())
        }
    }
}

fun Byte.toPositiveInt() = toInt() and 0xFF
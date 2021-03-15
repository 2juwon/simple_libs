package devdan.lib.usb.simple

import java.io.Serializable

data class AlcodiDeviceInfo(
    var vendorId: Int = 0,
    var productId: Int = 0,
    var serialNumber: String = "",
    var countryCode: String = "",
    var modelNumber: Int = 0,
    var productCode: String = "",
    var releaseDate: String = "",
    var correctionDate: String = "",
    var useCount: Int = 0
) : Serializable {
    fun getVendorIdHex(): String {
        return String.format("0x%04X", vendorId)
    }

    fun getProductIdHex(): String {
        return String.format("0x%04X", productId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlcodiDeviceInfo

        if (vendorId != other.vendorId) return false
        if (productId != other.productId) return false
        if (serialNumber != other.serialNumber) return false
        if (countryCode != other.countryCode) return false
        if (modelNumber != other.modelNumber) return false
        if (productCode != other.productCode) return false
        if (releaseDate != other.releaseDate) return false
        if (correctionDate != other.correctionDate) return false
        if (useCount != other.useCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vendorId
        result = 31 * result + productId
        result = 31 * result + serialNumber.hashCode()
        result = 31 * result + countryCode.hashCode()
        result = 31 * result + modelNumber
        result = 31 * result + productCode.hashCode()
        result = 31 * result + releaseDate.hashCode()
        result = 31 * result + correctionDate.hashCode()
        result = 31 * result + useCount
        return result
    }
}
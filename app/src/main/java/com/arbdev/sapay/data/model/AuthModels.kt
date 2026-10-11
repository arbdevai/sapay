package com.arbdev.sapay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
data class CheckPasswordRequest(
    @SerialName("creds") val creds: String
)

@Serializable
data class CheckPasswordResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null
)

@Serializable
data class LoginRequest(
    @SerialName("creds") val creds: String,
    @SerialName("password") val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("result") val result: String? = null
)

@Serializable
data class UserProfile(
    @SerialName("id") val id: String? = null,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("active_package_expire_date") val activePackageExpireDate: String? = null,
    @SerialName("settlement_date") val settlementDate: String? = null,
    @SerialName("trx_code") val trxCode: String? = null,
    @SerialName("active_package_id") val activePackageRaw: JsonElement? = null,
    @SerialName("active_regional_package_id") val activeRegionalPackageRaw: JsonElement? = null,
    val parsedPackageName: String? = null,
    val parsedSpeedUpTo: String? = null,
    val parsedPrice: Double? = null
) {
    val packageName: String
        get() {
            if (!parsedPackageName.isNullOrBlank()) return parsedPackageName
            val pkg = activePackageRaw
            if (pkg is JsonObject) {
                pkg["name"]?.let { if (it is JsonPrimitive) return it.content }
            } else if (pkg is JsonPrimitive && pkg.isString && pkg.content.isNotBlank()) {
                return pkg.content
            }
            val reg = activeRegionalPackageRaw
            if (reg is JsonObject) {
                reg["name"]?.let { if (it is JsonPrimitive) return it.content }
                val inner = reg["package_id"]
                if (inner is JsonObject) {
                    inner["name"]?.let { if (it is JsonPrimitive) return it.content }
                }
            }
            return "Paket Reguler"
        }

    val packageSpeed: String?
        get() {
            if (!parsedSpeedUpTo.isNullOrBlank()) return parsedSpeedUpTo
            val reg = activeRegionalPackageRaw
            if (reg is JsonObject) {
                reg["speed_up_to"]?.let { if (it is JsonPrimitive) return it.content }
            }
            return null
        }

    val packagePrice: Double?
        get() {
            if (parsedPrice != null && parsedPrice > 0) return parsedPrice
            val reg = activeRegionalPackageRaw
            if (reg is JsonObject) {
                val p = reg["price"]
                if (p is JsonPrimitive) {
                    return p.doubleOrNull ?: p.content.toDoubleOrNull()
                }
            }
            return null
        }

    val activeRegionalPackage: RegionalPackage?
        get() {
            val reg = activeRegionalPackageRaw
            val regObj = if (reg is JsonObject) reg else null

            val pkgId = regObj?.get("id")?.let { if (it is JsonPrimitive) it.content else null }
            val speed = packageSpeed
            val price = packagePrice
            val name = packageName

            return RegionalPackage(
                id = pkgId,
                name = name,
                price = price,
                speedUpTo = speed,
                duration = regObj?.get("duration")?.let { if (it is JsonPrimitive) it.intOrNull ?: it.content.toIntOrNull() else null },
                description = regObj?.get("description")?.let { if (it is JsonPrimitive) it.content else null },
                packageInfo = InnerPackage(id = pkgId, name = name)
            )
        }
}

@Serializable
data class ProfileResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("result") val result: JsonElement? = null,
    @SerialName("data") val data: JsonElement? = null
)

@Serializable
data class HistoryTransaction(
    @SerialName("trx_code") val trxCode: String? = null,
    @SerialName("payment_status") val paymentStatus: String? = null,
    @SerialName("settlement_date") val settlementDate: String? = null
)

@Serializable
data class BillHistoryItem(
    @SerialName("id") val id: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("amount") val amount: Double? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("is_paid") val isPaid: Boolean? = false,
    @SerialName("type") val type: String? = null,
    @SerialName("regional_package_id") val regionalPackage: RegionalPackage? = null,
    @SerialName("transaction_id") val transactionList: List<HistoryTransaction> = emptyList()
)

@Serializable
data class BillHistoryResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("total") val total: Int? = null,
    @SerialName("data") val data: List<BillHistoryItem> = emptyList(),
    @SerialName("message") val message: String? = null
)

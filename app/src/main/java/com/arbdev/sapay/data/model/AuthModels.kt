package com.arbdev.sapay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("active_package_id") val activePackageId: String? = null,
    @SerialName("active_regional_package_id") val activeRegionalPackage: RegionalPackage? = null
)

@Serializable
data class ProfileResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("result") val result: UserProfile? = null
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

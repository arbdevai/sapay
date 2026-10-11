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
data class BillHistoryItem(
    @SerialName("id") val id: String? = null,
    @SerialName("invoice_number") val invoiceNumber: String? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    @SerialName("payment_status") val paymentStatus: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("description") val description: String? = null
)

@Serializable
data class BillHistoryResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("result") val result: List<BillHistoryItem> = emptyList()
)

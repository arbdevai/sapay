package com.arbdev.sapay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MicrositeRequest(
    @SerialName("payload") val payload: String
)

@Serializable
data class ReactiveCustomerRequest(
    @SerialName("billing_date") val billingDate: String,
    @SerialName("payload") val payload: String
)

@Serializable
data class ReactiveCustomerResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: JsonElement? = null
)

@Serializable
data class InnerPackage(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("type") val type: String? = null
)

@Serializable
data class RegionalPackage(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("speed_up_to") val speedUpTo: String? = null,
    @SerialName("duration") val duration: Int? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("package_id") val packageInfo: InnerPackage? = null
)

@Serializable
data class CustomerInfo(
    @SerialName("id") val id: String? = null,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("mitra_id") val mitraId: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("active_package_expire_date") val activePackageExpireDate: String? = null,
    @SerialName("settlement_date") val settlementDate: String? = null,
    @SerialName("regional_package_id") val regionalPackage: RegionalPackage? = null,
    @SerialName("active_regional_package_id") val activeRegionalPackage: RegionalPackage? = null
)

@Serializable
data class MicrositeResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("customer") val customer: CustomerInfo? = null,
    @SerialName("data") val data: String? = null
)

@Serializable
data class PackageWrapper(
    @SerialName("reguler") val reguler: List<RegionalPackage> = emptyList(),
    @SerialName("sme") val sme: List<RegionalPackage> = emptyList()
)

@Serializable
data class ChangePackageListResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("result") val result: PackageWrapper? = null
)

@Serializable
data class PaymentChannelsResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: List<PaymentChannelGroup> = emptyList()
)

@Serializable
data class PaymentChannelGroup(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("payment_channel") val paymentChannels: List<PaymentChannelItem> = emptyList()
)

@Serializable
data class PaymentChannelItem(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("code_bank") val codeBank: String? = null,
    @SerialName("channel_image") val channelImage: String? = null,
    @SerialName("fee_type") val feeType: String? = null,
    @SerialName("fee_flat") val feeFlat: String? = null,
    @SerialName("fee_percent") val feePercent: String? = null,
    @SerialName("status") val status: String? = null
)

@Serializable
data class HandlePaymentRequest(
    @SerialName("payment_channel_id") val paymentChannelId: String,
    @SerialName("event") val event: String = "payment_package",
    @SerialName("regional_package_id") val regionalPackageId: String? = null,
    @SerialName("customer_id") val customerId: String
)

@Serializable
data class HandlePaymentResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: JsonElement? = null,
    @SerialName("result") val result: JsonElement? = null
)

@Serializable
data class CurrentPaymentRequest(
    @SerialName("customer_id") val customerId: String
)

@Serializable
data class VaDetail(
    @SerialName("va") val va: String? = null
)

@Serializable
data class PayloadAction(
    @SerialName("name") val name: String? = null,
    @SerialName("value") val value: String? = null
)

@Serializable
data class PaymentPayload(
    @SerialName("actions") val actions: List<PayloadAction> = emptyList()
)

@Serializable
data class CurrentPaymentResult(
    @SerialName("trx_code") val trxCode: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_status") val paymentStatus: String? = null,
    @SerialName("grand_total") val grandTotal: Double? = null,
    @SerialName("payment_number") val paymentNumber: String? = null,
    @SerialName("payment_code") val paymentCode: String? = null,
    @SerialName("va") val va: VaDetail? = null,
    @SerialName("payment_payload") val paymentPayload: PaymentPayload? = null
)

@Serializable
data class CurrentPaymentResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("result") val result: CurrentPaymentResult? = null
)

@Serializable
data class CheckStatusRequest(
    @SerialName("trx_code") val trxCode: String
)

@Serializable
data class CheckStatusResponse(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("result") val result: JsonElement? = null
)

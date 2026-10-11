package com.arbdev.sapay.data.repository

import com.arbdev.sapay.data.api.StarliteApi
import com.arbdev.sapay.data.model.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillingRepository(private val api: StarliteApi) {

    suspend fun checkBill(payload: String): Result<MicrositeResponse> {
        return try {
            val response = api.checkMicrositeBill(MicrositeRequest(payload = payload))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseApiError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(formatNetworkException(e))
        }
    }

    suspend fun reactivateCustomer(payload: String): Result<Boolean> {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val response = api.reactiveCustomer(ReactiveCustomerRequest(billingDate = today, payload = payload))
            if (response.isSuccessful || response.code() == 201) {
                Result.success(true)
            } else {
                val errorMsg = parseApiError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(formatNetworkException(e))
        }
    }

    suspend fun getAvailablePackages(customerId: String?, mitraId: String?): Result<List<RegionalPackage>> {
        return try {
            val response = api.getChangePackageList(customerId = customerId, mitraId = mitraId)
            if (response.isSuccessful && response.body()?.result != null) {
                val wrapper = response.body()!!.result!!
                val combined = mutableListOf<RegionalPackage>()
                combined.addAll(wrapper.reguler)
                combined.addAll(wrapper.sme)
                Result.success(combined)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(formatNetworkException(e))
        }
    }

    suspend fun getPaymentChannels(): Result<List<PaymentChannelGroup>> {
        return try {
            val response = api.getPaymentChannels()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                val errorMsg = parseApiError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(formatNetworkException(e))
        }
    }

    suspend fun createPayment(
        customerId: String,
        regionalPackageId: String?,
        channelId: String
    ): Result<HandlePaymentResponse> {
        return try {
            val response = api.handleCustomerPayment(
                HandlePaymentRequest(
                    paymentChannelId = channelId,
                    regionalPackageId = regionalPackageId,
                    customerId = customerId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseApiError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(formatNetworkException(e))
        }
    }

    suspend fun getCurrentPayment(customerId: String): Result<CurrentPaymentResult> {
        return try {
            val response = api.getCurrentPayment(CurrentPaymentRequest(customerId = customerId))
            if (response.isSuccessful && response.body()?.result != null) {
                Result.success(response.body()!!.result!!)
            } else {
                val errorMsg = parseApiError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(formatNetworkException(e))
        }
    }

    suspend fun checkPaymentStatus(trxCode: String, customerId: String): Result<PaymentCheckStatusResult> {
        return try {
            val response = api.checkPaymentStatus(
                CheckStatusRequest(
                    trxCode = trxCode,
                    customerId = customerId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val rootObj = body as? JsonObject
                val status = rootObj?.get("payment_status")?.let { if (it is JsonPrimitive) it.content else null }
                    ?: rootObj?.get("data")?.let {
                        if (it is JsonObject) it["payment_status"]?.let { p -> if (p is JsonPrimitive) p.content else null }
                        else if (it is JsonPrimitive) it.content else null
                    }
                    ?: rootObj?.get("result")?.let {
                        if (it is JsonObject) it["payment_status"]?.let { p -> if (p is JsonPrimitive) p.content else null }
                        else if (it is JsonPrimitive) it.content else null
                    }
                val isSettle = status?.equals("settle", ignoreCase = true) == true ||
                        status?.equals("success", ignoreCase = true) == true ||
                        status?.equals("paid", ignoreCase = true) == true
                Result.success(
                    PaymentCheckStatusResult(
                        isSettled = isSettle,
                        paymentStatus = status,
                        message = rootObj?.get("message")?.let { if (it is JsonPrimitive) it.content else null }
                    )
                )
            } else {
                val errorMsg = parseApiError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(formatNetworkException(e))
        }
    }

    private fun parseApiError(responseCode: Int, errorBody: String?): String {
        when (responseCode) {
            502, 503, 504 -> return "Server Starlite sedang dalam pemeliharaan (Maintenance). Silakan coba beberapa saat lagi."
            500 -> return "Terjadi gangguan sistem pada server Starlite. Silakan coba kembali sesaat lagi."
            401, 403 -> return "Akses ke server Starlite dibatasi atau sesi telah berakhir. Silakan coba kembali."
            404 -> return "Layanan Starlite yang diminta tidak ditemukan."
        }

        if (errorBody.isNullOrBlank()) {
            return "Terjadi kendala pada server (Status: $responseCode)"
        }

        val trimmed = errorBody.trim()
        if (trimmed.startsWith("<") || trimmed.contains("<html", ignoreCase = true) ||
            trimmed.contains("bad gateway", ignoreCase = true) || trimmed.contains("cloudflare", ignoreCase = true)) {
            return "Server Starlite sedang dalam pemeliharaan (Maintenance). Silakan coba beberapa saat lagi."
        }

        return try {
            val json = JSONObject(trimmed)
            json.optString("message", json.optString("error", "Gagal memproses permintaan"))
        } catch (_: Exception) {
            "Gagal memproses respons dari server Starlite"
        }
    }

    private fun formatNetworkException(e: Throwable): Exception {
        val msg = e.message ?: ""
        val friendlyMessage = when {
            e is java.net.SocketTimeoutException -> "Koneksi ke server Starlite terputus (Timeout). Periksa sinyal internet Anda."
            e is java.net.UnknownHostException || e is java.net.ConnectException -> "Tidak dapat terhubung ke server Starlite. Periksa koneksi internet Anda."
            msg.contains("502") || msg.contains("503") || msg.contains("504") || msg.contains("Bad Gateway", ignoreCase = true) ->
                "Server Starlite sedang dalam pemeliharaan (Maintenance). Silakan coba beberapa saat lagi."
            msg.contains("Unexpected JSON", ignoreCase = true) ->
                "Format data dari server Starlite sedang disesuaikan. Silakan coba kembali nanti."
            msg.isNotBlank() -> msg
            else -> "Terjadi kendala jaringan. Silakan coba beberapa saat lagi."
        }
        return Exception(friendlyMessage, e)
    }
}

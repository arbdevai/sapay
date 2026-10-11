package com.arbdev.sapay.data.repository

import com.arbdev.sapay.data.api.StarliteApi
import com.arbdev.sapay.data.model.*
import org.json.JSONObject

class BillingRepository(private val api: StarliteApi) {

    suspend fun checkBill(payload: String): Result<MicrositeResponse> {
        return try {
            val response = api.checkMicrositeBill(MicrositeRequest(payload = payload))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPaymentChannels(): Result<List<PaymentChannelGroup>> {
        return try {
            val response = api.getPaymentChannels()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Gagal mengambil metode pembayaran"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentPayment(customerId: String): Result<CurrentPaymentResult> {
        return try {
            val response = api.getCurrentPayment(CurrentPaymentRequest(customerId = customerId))
            if (response.isSuccessful && response.body()?.result != null) {
                Result.success(response.body()!!.result!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkPaymentStatus(trxCode: String): Result<Boolean> {
        return try {
            val response = api.checkPaymentStatus(CheckStatusRequest(trxCode = trxCode))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Gagal verifikasi status pembayaran"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Terjadi kesalahan pada server"
        return try {
            val json = JSONObject(errorBody)
            json.optString("message", "Terjadi kesalahan")
        } catch (_: Exception) {
            "Terjadi kesalahan: $errorBody"
        }
    }
}

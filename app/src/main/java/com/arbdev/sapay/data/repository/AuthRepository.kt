package com.arbdev.sapay.data.repository

import com.arbdev.sapay.data.api.StarliteApi
import com.arbdev.sapay.data.model.BillHistoryItem
import com.arbdev.sapay.data.model.CheckPasswordRequest
import com.arbdev.sapay.data.model.LoginRequest
import org.json.JSONObject

class AuthRepository(private val api: StarliteApi) {

    suspend fun checkPasswordExists(creds: String): Result<Boolean> {
        return try {
            val response = api.checkPassword(CheckPasswordRequest(creds = creds))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(creds: String, pass: String): Result<String> {
        return try {
            val response = api.login(LoginRequest(creds = creds, password = pass))
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

    suspend fun getHistory(token: String, customerId: String? = null): Result<List<BillHistoryItem>> {
        return try {
            val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = api.getBillingHistory(token = formattedToken, customerId = customerId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.result)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Terjadi kesalahan pada server"
        return try {
            val json = JSONObject(errorBody)
            json.optString("message", "Gagal memproses permintaan")
        } catch (_: Exception) {
            "Terjadi kesalahan"
        }
    }
}

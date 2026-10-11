package com.arbdev.sapay.data.repository

import com.arbdev.sapay.data.api.StarliteApi
import com.arbdev.sapay.data.model.BillHistoryItem
import com.arbdev.sapay.data.model.CheckPasswordRequest
import com.arbdev.sapay.data.model.LoginRequest
import com.arbdev.sapay.data.model.UserProfile
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

    suspend fun forgotPassword(creds: String): Result<String> {
        return try {
            val response = api.forgotPassword(CheckPasswordRequest(creds = creds))
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Link reset password telah dikirim ke WhatsApp Anda.")
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(token: String): Result<Unit> {
        return try {
            val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            api.logout(token = formattedToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    suspend fun getProfile(token: String): Result<UserProfile> {
        return try {
            val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = api.getProfile(token = formattedToken)
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

    suspend fun getHistory(token: String, page: Int = 1, pageSize: Int = 20): Result<List<BillHistoryItem>> {
        return try {
            val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = api.getBillingHistory(token = formattedToken, page = page, pageSize = pageSize)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
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

package com.arbdev.sapay.data.repository

import com.arbdev.sapay.data.api.StarliteApi
import com.arbdev.sapay.data.model.BillHistoryItem
import com.arbdev.sapay.data.model.CheckPasswordRequest
import com.arbdev.sapay.data.model.LoginRequest
import com.arbdev.sapay.data.model.UserProfile
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
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
            if (response.isSuccessful && response.body() != null) {
                val profile = parseUserProfile(response.body()!!)
                if (profile != null) {
                    Result.success(profile)
                } else {
                    Result.failure(Exception("Data profil tidak ditemukan"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseUserProfile(root: JsonElement): UserProfile? {
        val rootObj = root as? JsonObject ?: return null

        val target = when {
            rootObj["result"] is JsonObject -> rootObj["result"] as JsonObject
            rootObj["data"] is JsonObject -> rootObj["data"] as JsonObject
            rootObj["customer"] is JsonObject -> rootObj["customer"] as JsonObject
            else -> rootObj
        }

        fun JsonObject.optString(key: String): String? {
            val el = this[key] ?: return null
            if (el is JsonNull) return null
            if (el is JsonPrimitive) {
                val str = el.content
                return if (str.equals("null", ignoreCase = true) || str.isBlank()) null else str
            }
            return null
        }

        fun JsonObject.optDouble(key: String): Double? {
            val el = this[key] ?: return null
            if (el is JsonNull) return null
            if (el is JsonPrimitive) {
                return el.doubleOrNull ?: el.content.toDoubleOrNull()
            }
            return null
        }

        val id = target.optString("id")
        val customerId = target.optString("customer_id")
        val name = target.optString("name")
        val phone = target.optString("phone_number")
        val email = target.optString("email")
        val address = target.optString("address")
        val status = target.optString("status")
        val expireDate = target.optString("active_package_expire_date")
        val settlementDate = target.optString("settlement_date")
        val trxCode = target.optString("trx_code")

        val activePkgRaw = target["active_package_id"]
        val activeRegPkgRaw = target["active_regional_package_id"]

        var pkgName: String? = null
        if (activePkgRaw is JsonObject) {
            pkgName = activePkgRaw.optString("name")
        } else if (activePkgRaw is JsonPrimitive && activePkgRaw.isString) {
            pkgName = activePkgRaw.content
        }

        var speed: String? = null
        var price: Double? = null
        if (activeRegPkgRaw is JsonObject) {
            if (pkgName.isNullOrBlank()) {
                pkgName = activeRegPkgRaw.optString("name")
            }
            speed = activeRegPkgRaw.optString("speed_up_to")
            price = activeRegPkgRaw.optDouble("price")
            val innerPkg = activeRegPkgRaw["package_id"]
            if (pkgName.isNullOrBlank() && innerPkg is JsonObject) {
                pkgName = innerPkg.optString("name")
            }
        }

        return UserProfile(
            id = id,
            customerId = customerId,
            name = name,
            phoneNumber = phone,
            email = email,
            address = address,
            status = status,
            activePackageExpireDate = expireDate,
            settlementDate = settlementDate,
            trxCode = trxCode,
            activePackageRaw = activePkgRaw,
            activeRegionalPackageRaw = activeRegPkgRaw,
            parsedPackageName = pkgName ?: "Paket Reguler",
            parsedSpeedUpTo = speed,
            parsedPrice = price
        )
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

package com.arbdev.sapay.data.repository

import com.arbdev.sapay.data.api.GitHubApi
import com.arbdev.sapay.data.model.GitHubRelease

class UpdateRepository(private val api: GitHubApi) {

    suspend fun checkLatestRelease(currentVersionName: String): Result<GitHubRelease?> {
        return try {
            val response = api.getLatestRelease()
            if (response.isSuccessful && response.body() != null) {
                val release = response.body()!!
                val cleanRemoteTag = release.tagName.trim().removePrefix("v")
                val cleanCurrent = currentVersionName.trim().removePrefix("v")

                if (isNewerVersion(cleanRemoteTag, cleanCurrent)) {
                    Result.success(release)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}

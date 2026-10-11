package com.arbdev.sapay.data.api

import com.arbdev.sapay.data.model.GitHubRelease
import retrofit2.Response
import retrofit2.http.GET

interface GitHubApi {

    @GET("repos/arbdevai/sapay/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubRelease>
}

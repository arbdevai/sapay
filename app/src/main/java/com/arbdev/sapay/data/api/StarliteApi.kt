package com.arbdev.sapay.data.api

import com.arbdev.sapay.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface StarliteApi {

    @POST("app/transaction/payment-microsite")
    suspend fun checkMicrositeBill(
        @Body request: MicrositeRequest
    ): Response<MicrositeResponse>

    @POST("app/customer/reactive-customer")
    suspend fun reactiveCustomer(
        @Body request: ReactiveCustomerRequest
    ): Response<ReactiveCustomerResponse>

    @GET("app/package/change-package-list")
    suspend fun getChangePackageList(
        @Query("customer_id") customerId: String? = null,
        @Query("mitra_id") mitraId: String? = null
    ): Response<ChangePackageListResponse>

    @GET("app/transaction/payment-channels")
    suspend fun getPaymentChannels(): Response<List<PaymentChannelGroup>>

    @POST("app/billing/handle-all-customer-payment")
    suspend fun handleCustomerPayment(
        @Body request: HandlePaymentRequest
    ): Response<HandlePaymentResponse>

    @POST("app/transaction/current-payment")
    suspend fun getCurrentPayment(
        @Body request: CurrentPaymentRequest
    ): Response<CurrentPaymentResponse>

    @POST("app/transaction/check-payment-status")
    suspend fun checkPaymentStatus(
        @Body request: CheckStatusRequest
    ): Response<CheckStatusResponse>

    @POST("app/auth/check-password")
    suspend fun checkPassword(
        @Body request: CheckPasswordRequest
    ): Response<CheckPasswordResponse>

    @POST("app/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("app/auth/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>

    @GET("app/billing/history-billing")
    suspend fun getBillingHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BillHistoryResponse>
}

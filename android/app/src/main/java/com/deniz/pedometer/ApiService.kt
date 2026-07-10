package com.deniz.pedometer

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

data class RegisterRequest(val username: String, val password: String)
data class TokenResponse(val access_token: String, val token_type: String)
data class StepEntryDto(val device_id: String, val day: String, val steps: Int)
data class StepSyncRequest(val entries: List<StepEntryDto>)

interface ApiService {

    @POST("register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenResponse>

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    @POST("steps/sync")
    suspend fun syncSteps(
        @Header("Authorization") bearerToken: String,
        @Body body: StepSyncRequest
    ): Response<Unit>
}

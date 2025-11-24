package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.LoginRequest
import com.lc9th5.gym.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<Any> // Can be AuthResponse or error map

    @POST("api/auth/refresh")
    suspend fun refresh(@Header("Authorization") authHeader: String): Response<AuthResponse>

    // Optional: resend verification code to email
    @POST("api/auth/request-email-verification")
    suspend fun requestEmailVerification(@Query("email") email: String): Response<Map<String, String>>

    @POST("api/auth/verify-email-code")
    suspend fun verifyEmailCode(@Query("email") email: String, @Query("code") code: String): Response<Map<String, String>>
}

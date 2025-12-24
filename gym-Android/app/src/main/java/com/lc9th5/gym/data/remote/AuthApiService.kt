package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.LoginRequest
import com.lc9th5.gym.data.model.RegisterRequest
import com.lc9th5.gym.data.model.TwoFactorRequiredResponse
import com.lc9th5.gym.data.model.TwoFactorSetupResponse
import com.lc9th5.gym.data.model.TwoFactorVerifyResponse
import com.lc9th5.gym.data.model.TwoFactorStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<Any> // Can be AuthResponse or TwoFactorRequiredResponse

    // ==================== 2FA Login ====================
    
    /**
     * Verify 2FA code sau khi login (khi user co 2FA enabled)
     */
    @POST("auth/login/2fa")
    suspend fun verify2faLogin(@Body request: TwoFactorLoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Header("Authorization") authHeader: String): Response<AuthResponse>

    // Optional: resend verification code to email
    @POST("auth/request-email-verification")
    suspend fun requestEmailVerification(@Query("email") email: String): Response<Map<String, String>>

    @POST("auth/verify-email-code")
    suspend fun verifyEmailCode(@Query("email") email: String, @Query("code") code: String): Response<Map<String, String>>
    
    // ==================== Profile Management ====================
    
    /**
     * Cap nhat thong tin ho so
     */
    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<Map<String, Any>>
    
    /**
     * Doi mat khau (can 2FA neu da bat)
     */
    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Map<String, Any>>
    
    // ==================== 2FA Management ====================
    
    /**
     * Lay trang thai 2FA cua user
     */
    @GET("2fa/status")
    suspend fun get2faStatus(): Response<TwoFactorStatusResponse>
    
    /**
     * Buoc 1: Khoi tao setup 2FA - nhan QR code
     */
    @POST("2fa/setup")
    suspend fun setup2fa(): Response<TwoFactorSetupResponse>
    
    /**
     * Buoc 2: Verify va enable 2FA
     */
    @POST("2fa/verify")
    suspend fun verify2fa(@Body request: TwoFactorVerifyRequest): Response<TwoFactorVerifyResponse>
    
    /**
     * Tat 2FA
     */
    @POST("2fa/disable")
    suspend fun disable2fa(@Body request: TwoFactorVerifyRequest): Response<Map<String, Any>>
    
    /**
     * Tao backup codes moi
     */
    @POST("2fa/backup/regenerate")
    suspend fun regenerateBackupCodes(@Body request: TwoFactorVerifyRequest): Response<Map<String, Any>>
}

// ==================== Request DTOs ====================

data class TwoFactorLoginRequest(
    val tempToken: String,
    val code: String
)

data class TwoFactorVerifyRequest(
    val code: String
)

data class UpdateProfileRequest(
    val fullName: String? = null,
    val username: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val twoFactorCode: String? = null
)

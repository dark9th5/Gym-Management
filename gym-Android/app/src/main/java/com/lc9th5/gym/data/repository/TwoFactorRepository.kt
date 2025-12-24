package com.lc9th5.gym.data.repository

import com.google.gson.Gson
import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.TwoFactorRequiredResponse
import com.lc9th5.gym.data.model.TwoFactorSetupResponse
import com.lc9th5.gym.data.model.TwoFactorStatusResponse
import com.lc9th5.gym.data.model.TwoFactorVerifyResponse
import com.lc9th5.gym.data.remote.AuthApiService
import com.lc9th5.gym.data.remote.TwoFactorLoginRequest
import com.lc9th5.gym.data.remote.TwoFactorVerifyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository xử lý các API 2FA
 */
class TwoFactorRepository(
    private val authApiService: AuthApiService
) {
    private val gson = Gson()
    
    /**
     * Lấy trạng thái 2FA của user hiện tại
     */
    suspend fun get2faStatus(): Result<TwoFactorStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.get2faStatus()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Không thể lấy trạng thái 2FA"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bước 1: Khởi tạo setup 2FA - nhận QR code
     */
    suspend fun setup2fa(): Result<TwoFactorSetupResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.setup2fa()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Không thể thiết lập 2FA"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bước 2: Verify và enable 2FA
     */
    suspend fun verify2fa(code: String): Result<TwoFactorVerifyResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.verify2fa(TwoFactorVerifyRequest(code))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Mã xác thực không đúng"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Tắt 2FA
     */
    suspend fun disable2fa(code: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.disable2fa(TwoFactorVerifyRequest(code))
            if (response.isSuccessful) {
                val body = response.body()
                val success = body?.get("success") as? Boolean ?: false
                if (success) {
                    Result.success(true)
                } else {
                    val error = body?.get("error") as? String ?: "Không thể tắt 2FA"
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Không thể tắt 2FA"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Verify 2FA code khi login
     */
    suspend fun verify2faLogin(tempToken: String, code: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.verify2faLogin(TwoFactorLoginRequest(tempToken, code))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Mã xác thực không đúng"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Tạo backup codes mới
     */
    suspend fun regenerateBackupCodes(code: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.regenerateBackupCodes(TwoFactorVerifyRequest(code))
            if (response.isSuccessful) {
                val body = response.body()
                val success = body?.get("success") as? Boolean ?: false
                if (success) {
                    @Suppress("UNCHECKED_CAST")
                    val backupCodes = body?.get("backupCodes") as? List<String> ?: emptyList()
                    Result.success(backupCodes)
                } else {
                    val error = body?.get("error") as? String ?: "Không thể tạo backup codes"
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Không thể tạo backup codes"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val errorMap = gson.fromJson(errorBody, Map::class.java)
            errorMap["error"] as? String ?: errorMap["message"] as? String
        } catch (e: Exception) {
            null
        }
    }
}

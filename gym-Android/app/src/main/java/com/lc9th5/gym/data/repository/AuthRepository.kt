package com.lc9th5.gym.data.repository

import com.google.gson.Gson
import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.LoginRequest
import com.lc9th5.gym.data.model.RegisterRequest
import com.lc9th5.gym.data.model.TwoFactorRequiredResponse
import com.lc9th5.gym.data.remote.AuthApiService
import com.lc9th5.gym.data.remote.TwoFactorLoginRequest
import retrofit2.Response

/**
 * Kết quả login có thể là:
 * 1. AuthResponse - Đăng nhập thành công
 * 2. TwoFactorRequiredResponse - Cần verify 2FA
 */
sealed class LoginResult {
    data class Success(val response: AuthResponse) : LoginResult()
    data class Requires2FA(val tempToken: String, val message: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class AuthRepository(private val apiService: AuthApiService) {
    
    private val gson = Gson()

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Parse error response to get proper error message
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Đăng ký thất bại"
                    } else {
                        when (response.code()) {
                            400 -> "Thông tin không hợp lệ"
                            409 -> "Email hoặc username đã tồn tại"
                            else -> "Đăng ký thất bại"
                        }
                    }
                } catch (e: Exception) {
                    "Đăng ký thất bại"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }

    /**
     * Login với hỗ trợ 2FA
     * Trả về LoginResult thay vì Result<AuthResponse>
     */
    suspend fun login(request: LoginRequest): LoginResult {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val jsonString = gson.toJson(response.body())
                
                // Kiểm tra xem response có phải là 2FA required không
                val bodyMap = gson.fromJson(jsonString, Map::class.java)
                
                if (bodyMap["requires2fa"] == true) {
                    // Cần 2FA verification
                    val tempToken = bodyMap["tempToken"] as? String ?: ""
                    val message = bodyMap["message"] as? String ?: "Vui lòng nhập mã xác thực"
                    LoginResult.Requires2FA(tempToken, message)
                } else {
                    // Đăng nhập thành công
                    val authResponse = gson.fromJson(jsonString, AuthResponse::class.java)
                    LoginResult.Success(authResponse)
                }
            } else {
                // Parse error response to get proper error message
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Đăng nhập thất bại"
                    } else {
                        when (response.code()) {
                            401 -> "Email hoặc mật khẩu không đúng"
                            403 -> "Tài khoản chưa được xác thực. Vui lòng kiểm tra email"
                            else -> "Đăng nhập thất bại"
                        }
                    }
                } catch (e: Exception) {
                    "Đăng nhập thất bại"
                }
                LoginResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            LoginResult.Error("Lỗi kết nối: ${e.message}")
        }
    }
    
    /**
     * Verify 2FA code và hoàn tất login
     */
    suspend fun verify2FALogin(tempToken: String, code: String): Result<AuthResponse> {
        return try {
            val response = apiService.verify2faLogin(TwoFactorLoginRequest(tempToken, code))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Mã xác thực không đúng"
                    } else {
                        when (response.code()) {
                            401 -> "Mã xác thực không đúng hoặc đã hết hạn"
                            else -> "Xác thực thất bại"
                        }
                    }
                } catch (e: Exception) {
                    "Xác thực thất bại"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }

    suspend fun refresh(token: String): Result<AuthResponse> {
        return try {
            val response = apiService.refresh("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Parse error response to get proper error message
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Làm mới token thất bại"
                    } else {
                        when (response.code()) {
                            401 -> "Token không hợp lệ hoặc đã hết hạn"
                            else -> "Làm mới token thất bại"
                        }
                    }
                } catch (e: Exception) {
                    "Làm mới token thất bại"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }

    suspend fun requestEmailVerification(email: String): Result<String> {
        return try {
            val response = apiService.requestEmailVerification(email)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val message = body["message"] ?: "Đã gửi mã xác thực"
                Result.success(message)
            } else {
                // Parse error response to get proper error message
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Gửi mã xác thực thất bại"
                    } else {
                        when (response.code()) {
                            400 -> "Email không hợp lệ"
                            404 -> "Email không tồn tại"
                            else -> "Gửi mã xác thực thất bại"
                        }
                    }
                } catch (e: Exception) {
                    "Gửi mã xác thực thất bại"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }

    suspend fun verifyEmailCode(email: String, code: String): Result<String> {
        return try {
            val response = apiService.verifyEmailCode(email, code)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val message = body["message"] ?: "Email đã được xác thực"
                Result.success(message)
            } else {
                // Parse error response to get proper error message
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Xác thực thất bại"
                    } else {
                        when (response.code()) {
                            400 -> "Mã xác thực không hợp lệ"
                            404 -> "Email không tồn tại"
                            else -> "Xác thực thất bại"
                        }
                    }
                } catch (e: Exception) {
                    "Xác thực thất bại"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }

}

package com.lc9th5.gym.data.repository

import com.google.gson.Gson
import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.LoginRequest
import com.lc9th5.gym.data.model.RegisterRequest
import com.lc9th5.gym.data.remote.AuthApiService
import retrofit2.Response



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

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                // Convert Any to AuthResponse using Gson
                val jsonString = gson.toJson(response.body())
                val authResponse = gson.fromJson(jsonString, AuthResponse::class.java)
                Result.success(authResponse)
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

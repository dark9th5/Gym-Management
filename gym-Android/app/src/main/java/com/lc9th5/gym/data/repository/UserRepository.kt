package com.lc9th5.gym.data.repository

import com.google.gson.Gson
import com.lc9th5.gym.data.model.User
import com.lc9th5.gym.data.remote.UpdateProfileRequest
import com.lc9th5.gym.data.remote.UserApiService

/**
 * Repository for user-related operations
 */
class UserRepository(private val apiService: UserApiService) {
    
    private val gson = Gson()
    
    /**
     * Get current user profile
     */
    suspend fun getCurrentUser(token: String): Result<User> {
        return try {
            val response = apiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Không thể tải thông tin người dùng"
                    } else {
                        when (response.code()) {
                            401 -> "Phiên đăng nhập đã hết hạn"
                            404 -> "Không tìm thấy người dùng"
                            else -> "Không thể tải thông tin người dùng"
                        }
                    }
                } catch (e: Exception) {
                    "Không thể tải thông tin người dùng"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: Long, token: String): Result<User> {
        return try {
            val response = apiService.getUserById(userId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Không thể tải thông tin người dùng"
                    } else {
                        when (response.code()) {
                            401 -> "Phiên đăng nhập đã hết hạn"
                            404 -> "Không tìm thấy người dùng"
                            else -> "Không thể tải thông tin người dùng"
                        }
                    }
                } catch (e: Exception) {
                    "Không thể tải thông tin người dùng"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(fullName: String?, token: String): Result<User> {
        return try {
            val request = UpdateProfileRequest(fullName)
            val response = apiService.updateProfile(request, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        errorMap["error"] as? String ?: errorMap["message"] as? String ?: "Không thể cập nhật thông tin"
                    } else {
                        when (response.code()) {
                            401 -> "Phiên đăng nhập đã hết hạn"
                            400 -> "Thông tin không hợp lệ"
                            else -> "Không thể cập nhật thông tin"
                        }
                    }
                } catch (e: Exception) {
                    "Không thể cập nhật thông tin"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
}

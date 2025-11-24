package com.lc9th5.gym.data.repository

import com.google.gson.Gson
import com.lc9th5.gym.data.model.*
import com.lc9th5.gym.data.remote.FamilyApiService

/**
 * Repository for family-related operations
 */
class FamilyRepository(private val apiService: FamilyApiService) {
    
    private val gson = Gson()
    
    /**
     * Create a new family
     */
    suspend fun createFamily(name: String, description: String?, token: String): Result<Family> {
        return try {
            val request = CreateFamilyRequest(name, description)
            val response = apiService.createFamily(request, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Get all families for current user
     */
    suspend fun getUserFamilies(token: String): Result<List<Family>> {
        return try {
            val response = apiService.getUserFamilies("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Get family by ID with details
     */
    suspend fun getFamilyById(familyId: Long, token: String): Result<FamilyDetail> {
        return try {
            val response = apiService.getFamilyById(familyId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Update family
     */
    suspend fun updateFamily(
        familyId: Long,
        name: String?,
        description: String?,
        token: String
    ): Result<Family> {
        return try {
            val request = UpdateFamilyRequest(name, description)
            val response = apiService.updateFamily(familyId, request, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Delete family
     */
    suspend fun deleteFamily(familyId: Long, token: String): Result<String> {
        return try {
            val response = apiService.deleteFamily(familyId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val message = response.body()!!["message"] ?: "Xóa gia đình thành công"
                Result.success(message)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Get family members
     */
    suspend fun getFamilyMembers(familyId: Long, token: String): Result<List<FamilyMemberInfo>> {
        return try {
            val response = apiService.getFamilyMembers(familyId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Add member to family
     */
    suspend fun addMember(
        familyId: Long,
        userEmail: String,
        role: String,
        relationship: String?,
        token: String
    ): Result<FamilyMemberInfo> {
        return try {
            val request = AddMemberRequest(userEmail, role, relationship)
            val response = apiService.addMember(familyId, request, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Remove member from family
     */
    suspend fun removeMember(familyId: Long, userId: Long, token: String): Result<String> {
        return try {
            val response = apiService.removeMember(familyId, userId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val message = response.body()!!["message"] ?: "Xóa thành viên thành công"
                Result.success(message)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Update member role
     */
    suspend fun updateMemberRole(
        familyId: Long,
        userId: Long,
        role: String,
        token: String
    ): Result<FamilyMemberInfo> {
        return try {
            val request = UpdateMemberRoleRequest(role)
            val response = apiService.updateMemberRole(familyId, userId, request, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = parseError(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message}"))
        }
    }
    
    /**
     * Parse error response
     */
    private fun parseError(errorBody: String?, code: Int): String {
        return try {
            if (errorBody != null) {
                val errorMap = gson.fromJson(errorBody, Map::class.java)
                errorMap["error"] as? String ?: errorMap["message"] as? String ?: getDefaultError(code)
            } else {
                getDefaultError(code)
            }
        } catch (e: Exception) {
            getDefaultError(code)
        }
    }
    
    /**
     * Get default error message by HTTP code
     */
    private fun getDefaultError(code: Int): String {
        return when (code) {
            400 -> "Dữ liệu không hợp lệ"
            401 -> "Phiên đăng nhập đã hết hạn"
            403 -> "Bạn không có quyền thực hiện thao tác này"
            404 -> "Không tìm thấy"
            409 -> "Xung đột dữ liệu"
            else -> "Có lỗi xảy ra"
        }
    }
}

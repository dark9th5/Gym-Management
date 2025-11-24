package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * API service for user-related endpoints
 */
interface UserApiService {
    
    /**
     * Get current user profile
     */
    @GET("api/user/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authHeader: String
    ): Response<User>
    
    /**
     * Get user by ID
     */
    @GET("api/user/{id}")
    suspend fun getUserById(
        @Path("id") userId: Long,
        @Header("Authorization") authHeader: String
    ): Response<User>
    
    /**
     * Update user profile
     */
    @retrofit2.http.PUT("api/user/me")
    suspend fun updateProfile(
        @retrofit2.http.Body updateRequest: UpdateProfileRequest,
        @Header("Authorization") authHeader: String
    ): Response<User>
}

/**
 * Request body for updating user profile
 */
data class UpdateProfileRequest(
    val fullName: String? = null
)

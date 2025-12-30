package com.lc9th5.gym.data.repository

import com.lc9th5.gym.data.model.*
import com.lc9th5.gym.data.remote.AdminApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminRepository(private val api: AdminApiService) {

    suspend fun getDashboardStats(): Result<AdminDashboardStats> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDashboardStats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch stats: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<UserSummary>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllUsers()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch users: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun lockUser(userId: Long, reason: String): Result<UserSummary> = withContext(Dispatchers.IO) {
        try {
            val request = LockUserRequest(reason)
            val response = api.lockUser(userId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to lock user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockUser(userId: Long): Result<UserSummary> = withContext(Dispatchers.IO) {
        try {
            val response = api.unlockUser(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to unlock user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendNotification(title: String, content: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val req = CreateNotificationRequest(title, content)
            val response = api.sendNotification(req)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to send notification: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

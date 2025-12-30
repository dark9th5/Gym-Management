package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApiService {

    @GET("api/admin/dashboard/stats")
    suspend fun getDashboardStats(): Response<AdminDashboardStats>

    @GET("api/admin/users")
    suspend fun getAllUsers(): Response<List<UserSummary>>

    @POST("api/admin/users/{userId}/lock")
    suspend fun lockUser(@Path("userId") userId: Long, @Body req: LockUserRequest): Response<UserSummary>

    @POST("api/admin/users/{userId}/unlock")
    suspend fun unlockUser(@Path("userId") userId: Long): Response<UserSummary>

    @POST("api/admin/notifications")
    suspend fun sendNotification(@Body req: CreateNotificationRequest): Response<Any>

    // Exercises
    @POST("api/admin/exercises")
    suspend fun createExerciseTemplate(@Body req: UpsertExerciseTemplateRequest): Response<Any>

    @PUT("api/admin/exercises/{id}")
    suspend fun updateExerciseTemplate(@Path("id") id: Long, @Body req: UpsertExerciseTemplateRequest): Response<Any>

    @DELETE("api/admin/exercises/{id}")
    suspend fun deleteExerciseTemplate(@Path("id") id: Long): Response<Any>
}

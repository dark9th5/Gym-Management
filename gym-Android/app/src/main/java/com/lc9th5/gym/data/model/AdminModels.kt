package com.lc9th5.gym.data.model

data class AdminDashboardStats(
    val totalUsers: Long,
    val totalExercises: Long,
    val totalWorkoutsLogged: Long,
    val newUsersToday: Long
)

data class UserSummary(
    val id: Long,
    val email: String,
    val fullName: String?,
    val isLocked: Boolean,
    val createdAt: String? // LocalDateTime string
)

data class LockUserRequest(
    val reason: String
)

data class CreateNotificationRequest(
    val title: String,
    val content: String,
    val targetAudience: String = "ALL"
)

data class UpsertExerciseTemplateRequest(
    val title: String,
    val categoryId: Long,
    val description: String,
    val lessonNumber: Int? = null
)

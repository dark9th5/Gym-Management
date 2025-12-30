package lc._th5.gym_BE.dto

import java.time.LocalDateTime

// ==================== STATS ====================
data class AdminDashboardStats(
    val totalUsers: Long,
    val totalExercises: Long,
    val totalWorkoutsLogged: Long,
    val newUsersToday: Long
)

// ==================== USER MANAGEMENT ====================
data class UserSummaryDto(
    val id: Long,
    val email: String,
    val fullName: String?,
    val isLocked: Boolean,
    val createdAt: LocalDateTime?
)

data class LockUserRequest(
    val reason: String // For logging/audit purposes
)

// ==================== NOTIFICATIONS ====================
data class CreateNotificationRequest(
    val title: String,
    val content: String,
    val targetAudience: String = "ALL"
)

// ==================== EXERCISE TEMPLATE (GUIDANCE) ====================
data class UpsertExerciseTemplateRequest(
    val title: String,
    val categoryId: Long,
    val description: String,
    val lessonNumber: Int? = null // Optional, if null auto-generate
)

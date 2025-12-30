package lc._th5.gym_BE.service

import lc._th5.gym_BE.dto.*
import lc._th5.gym_BE.model.guidance.GuidanceLesson
import lc._th5.gym_BE.model.notification.SystemNotification
import lc._th5.gym_BE.repository.UserRepository
import lc._th5.gym_BE.repository.guidance.GuidanceCategoryRepository
import lc._th5.gym_BE.repository.guidance.GuidanceLessonRepository
import lc._th5.gym_BE.repository.notification.SystemNotificationRepository
import lc._th5.gym_BE.repository.workout.WorkoutSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val lessonRepository: GuidanceLessonRepository,
    private val categoryRepository: GuidanceCategoryRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val notificationRepository: SystemNotificationRepository
) {

    // ==================== DASHBOARD & STATS ====================
    
    fun getDashboardStats(): AdminDashboardStats {
        val totalUsers = userRepository.count()
        val totalExercises = lessonRepository.count()
        val totalWorkouts = workoutSessionRepository.count()
        
        // Count new users today (Requires native query or findAll stream if not optimized)
        // Optimization: Add method to repo if needed. For now stream (not scaleable but works for demo)
        // Better: Use a custom query method in repo, but here we assume small scale
        val newUsersToday = userRepository.findAll().stream()
            .filter { it.createdAt?.toLocalDate()?.isEqual(LocalDate.now()) == true }
            .count()

        return AdminDashboardStats(
            totalUsers = totalUsers,
            totalExercises = totalExercises,
            totalWorkoutsLogged = totalWorkouts,
            newUsersToday = newUsersToday
        )
    }

    // ==================== USER MANAGEMENT ====================

    @Transactional
    fun lockUser(userId: Long): UserSummaryDto {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        user.isLocked = true
        // user.isVerified = false // Optional: force re-verify?
        val saved = userRepository.save(user)
        
        return UserSummaryDto(saved.id, saved.email, saved.fullName, saved.isLocked, saved.createdAt)
    }

    @Transactional
    fun unlockUser(userId: Long): UserSummaryDto {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
            
        user.isLocked = false
        val saved = userRepository.save(user)
        
        return UserSummaryDto(saved.id, saved.email, saved.fullName, saved.isLocked, saved.createdAt)
    }
    
    fun getAllUsers(): List<UserSummaryDto> {
        return userRepository.findAll().map { 
            UserSummaryDto(it.id, it.email, it.fullName, it.isLocked, it.createdAt)
        }
    }

    // ==================== NOTIFICATIONS ====================

    @Transactional
    fun createSystemNotification(request: CreateNotificationRequest): SystemNotification {
        val notification = SystemNotification(
            title = request.title,
            content = request.content,
            targetAudience = request.targetAudience
        )
        return notificationRepository.save(notification)
    }

    // ==================== EXERCISE TEMPLATES (GUIDANCE LESSONS) ====================

    @Transactional
    fun createExerciseTemplate(request: UpsertExerciseTemplateRequest): GuidanceLesson {
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { IllegalArgumentException("Category not found") }

        val lessonNumber = request.lessonNumber ?: (lessonRepository.count() + 1).toInt()

        val lesson = GuidanceLesson(
            title = request.title,
            description = request.description,
            category = category,
            lessonNumber = lessonNumber
        )
        return lessonRepository.save(lesson)
    }

    @Transactional
    fun updateExerciseTemplate(id: Long, request: UpsertExerciseTemplateRequest): GuidanceLesson {
        val lesson = lessonRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }
            
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { IllegalArgumentException("Category not found") }

        // Create copy with new values since data classes are immutable
        val updatedLesson = lesson.copy(
            title = request.title,
            description = request.description,
            category = category,
            lessonNumber = request.lessonNumber ?: lesson.lessonNumber
        )
        
        return lessonRepository.save(updatedLesson)
    }
    
    @Transactional
    fun deleteExerciseTemplate(id: Long) {
        if (!lessonRepository.existsById(id)) {
            throw IllegalArgumentException("Template not found")
        }
        lessonRepository.deleteById(id)
    }
}

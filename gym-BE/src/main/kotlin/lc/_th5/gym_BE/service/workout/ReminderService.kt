package lc._th5.gym_BE.service.workout

import lc._th5.gym_BE.model.workout.DayOfWeek
import lc._th5.gym_BE.model.workout.WorkoutReminder
import lc._th5.gym_BE.repository.UserRepository
import lc._th5.gym_BE.repository.workout.WorkoutReminderRepository
import lc._th5.gym_BE.util.WorkoutEncryptionHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
class ReminderService(
    private val reminderRepository: WorkoutReminderRepository,
    private val userRepository: UserRepository,
    private val encryptionHelper: WorkoutEncryptionHelper
) {
    
    @Transactional
    fun createReminder(userId: Long, request: CreateReminderRequest): WorkoutReminder {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        // Mã hóa title và message
        val reminder = WorkoutReminder(
            user = user,
            title = encryptionHelper.encryptName(request.title),
            message = encryptionHelper.encryptNullable(request.message),
            reminderTime = request.reminderTime,
            daysOfWeek = request.daysOfWeek.toMutableSet(),
            isEnabled = request.isEnabled
        )
        
        return reminderRepository.save(reminder)
    }
    
    fun getUserReminders(userId: Long): List<WorkoutReminder> {
        return reminderRepository.findByUserIdOrderByReminderTime(userId)
    }
    
    fun getEnabledReminders(userId: Long): List<WorkoutReminder> {
        return reminderRepository.findByUserIdAndIsEnabledTrue(userId)
    }
    
    @Transactional
    fun updateReminder(reminderId: Long, userId: Long, request: UpdateReminderRequest): WorkoutReminder {
        val reminder = getReminderByIdAndUser(reminderId, userId)
        
        request.title?.let { /* immutable in data class, need to recreate */ }
        request.isEnabled?.let { reminder.isEnabled = it }
        
        // Mã hóa các trường khi cập nhật
        val updatedReminder = reminder.copy(
            title = request.title?.let { encryptionHelper.encryptName(it) } ?: reminder.title,
            message = request.message?.let { encryptionHelper.encryptNullable(it) } ?: reminder.message,
            reminderTime = request.reminderTime ?: reminder.reminderTime,
            daysOfWeek = request.daysOfWeek?.toMutableSet() ?: reminder.daysOfWeek,
            isEnabled = request.isEnabled ?: reminder.isEnabled
        )
        
        return reminderRepository.save(updatedReminder)
    }
    
    @Transactional
    fun toggleReminder(reminderId: Long, userId: Long): WorkoutReminder {
        val reminder = getReminderByIdAndUser(reminderId, userId)
        reminder.isEnabled = !reminder.isEnabled
        return reminderRepository.save(reminder)
    }
    
    @Transactional
    fun deleteReminder(reminderId: Long, userId: Long) {
        val reminder = getReminderByIdAndUser(reminderId, userId)
        reminderRepository.delete(reminder)
    }
    
    // Cho scheduler sử dụng để gửi notification
    fun getRemindersForToday(): List<WorkoutReminder> {
        val today = java.time.LocalDate.now().dayOfWeek
        val dayOfWeek = DayOfWeek.valueOf(today.name)
        return reminderRepository.findEnabledRemindersByDayOfWeek(dayOfWeek)
    }
    
    private fun getReminderByIdAndUser(reminderId: Long, userId: Long): WorkoutReminder {
        val reminder = reminderRepository.findById(reminderId)
            .orElseThrow { IllegalArgumentException("Reminder not found") }
        
        if (reminder.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        return reminder
    }
}

// Request DTOs
data class CreateReminderRequest(
    val title: String,
    val message: String? = null,
    val reminderTime: LocalTime,
    val daysOfWeek: Set<DayOfWeek>,
    val isEnabled: Boolean = true
)

data class UpdateReminderRequest(
    val title: String? = null,
    val message: String? = null,
    val reminderTime: LocalTime? = null,
    val daysOfWeek: Set<DayOfWeek>? = null,
    val isEnabled: Boolean? = null
)

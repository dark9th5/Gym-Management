package lc._th5.gym_BE.controller.workout

import lc._th5.gym_BE.model.workout.DayOfWeek
import lc._th5.gym_BE.model.workout.WorkoutReminder
import lc._th5.gym_BE.service.workout.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalTime

@RestController
@RequestMapping("/api/workout/reminders")
class ReminderController(
    private val reminderService: ReminderService
) {
    
    @PostMapping
    fun createReminder(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: CreateReminderRequest
    ): ResponseEntity<ReminderResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val reminder = reminderService.createReminder(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(reminder.toResponse())
    }
    
    @GetMapping
    fun getReminders(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<ReminderResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        val reminders = reminderService.getUserReminders(userId)
        return ResponseEntity.ok(reminders.map { it.toResponse() })
    }
    
    @GetMapping("/enabled")
    fun getEnabledReminders(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<ReminderResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        val reminders = reminderService.getEnabledReminders(userId)
        return ResponseEntity.ok(reminders.map { it.toResponse() })
    }
    
    @PutMapping("/{reminderId}")
    fun updateReminder(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable reminderId: Long,
        @RequestBody request: UpdateReminderRequest
    ): ResponseEntity<ReminderResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val reminder = reminderService.updateReminder(reminderId, userId, request)
        return ResponseEntity.ok(reminder.toResponse())
    }
    
    @PostMapping("/{reminderId}/toggle")
    fun toggleReminder(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable reminderId: Long
    ): ResponseEntity<ReminderResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val reminder = reminderService.toggleReminder(reminderId, userId)
        return ResponseEntity.ok(reminder.toResponse())
    }
    
    @DeleteMapping("/{reminderId}")
    fun deleteReminder(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable reminderId: Long
    ): ResponseEntity<Void> {
        val userId = jwt.getClaim<Long>("uid")
        reminderService.deleteReminder(reminderId, userId)
        return ResponseEntity.noContent().build()
    }
}

// Response DTO
data class ReminderResponse(
    val id: Long,
    val title: String,
    val message: String?,
    val reminderTime: LocalTime,
    val daysOfWeek: Set<DayOfWeek>,
    val isEnabled: Boolean
)

fun WorkoutReminder.toResponse() = ReminderResponse(
    id = id,
    title = title,
    message = message,
    reminderTime = reminderTime,
    daysOfWeek = daysOfWeek,
    isEnabled = isEnabled
)

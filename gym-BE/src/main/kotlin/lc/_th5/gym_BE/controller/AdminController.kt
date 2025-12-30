package lc._th5.gym_BE.controller

import lc._th5.gym_BE.dto.*
import lc._th5.gym_BE.service.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Hard shield: Only Admins can touch this controller
class AdminController(
    private val adminService: AdminService
) {

    // ==================== DASHBOARD ====================
    @GetMapping("/dashboard/stats")
    fun getStats(): ResponseEntity<AdminDashboardStats> {
        return ResponseEntity.ok(adminService.getDashboardStats())
    }

    // ==================== USER MANAGEMENT ====================
    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<List<UserSummaryDto>> {
        return ResponseEntity.ok(adminService.getAllUsers())
    }

    @PostMapping("/users/{userId}/lock")
    fun lockUser(@PathVariable userId: Long, @RequestBody req: LockUserRequest): ResponseEntity<UserSummaryDto> {
        // Log reason somewhere if needed
        return ResponseEntity.ok(adminService.lockUser(userId))
    }

    @PostMapping("/users/{userId}/unlock")
    fun unlockUser(@PathVariable userId: Long): ResponseEntity<UserSummaryDto> {
        return ResponseEntity.ok(adminService.unlockUser(userId))
    }

    // ==================== NOTIFICATIONS ====================
    @PostMapping("/notifications")
    fun sendNotification(@RequestBody req: CreateNotificationRequest): ResponseEntity<Any> {
        val notif = adminService.createSystemNotification(req)
        return ResponseEntity.ok(mapOf(
            "message" to "Notification created successfully",
            "id" to notif.id
        ))
    }

    // ==================== EXERCISE TEMPLATES ====================
    @PostMapping("/exercises")
    fun createExercise(@RequestBody req: UpsertExerciseTemplateRequest): ResponseEntity<Any> {
        val created = adminService.createExerciseTemplate(req)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/exercises/{id}")
    fun updateExercise(@PathVariable id: Long, @RequestBody req: UpsertExerciseTemplateRequest): ResponseEntity<Any> {
        val updated = adminService.updateExerciseTemplate(id, req)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/exercises/{id}")
    fun deleteExercise(@PathVariable id: Long): ResponseEntity<Any> {
        adminService.deleteExerciseTemplate(id)
        return ResponseEntity.ok(mapOf("message" to "Exercise template deleted"))
    }
}

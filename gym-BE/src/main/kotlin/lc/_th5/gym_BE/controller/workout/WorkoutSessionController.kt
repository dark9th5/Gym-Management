package lc._th5.gym_BE.controller.workout

import lc._th5.gym_BE.model.workout.WorkoutSession
import lc._th5.gym_BE.model.workout.WorkoutExercise
import lc._th5.gym_BE.model.workout.WorkoutSet
import lc._th5.gym_BE.service.workout.*
import lc._th5.gym_BE.util.WorkoutEncryptionHelper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/workout/sessions")
class WorkoutSessionController(
    private val sessionService: WorkoutSessionService,
    private val encryptionHelper: WorkoutEncryptionHelper
) {
    
    @PostMapping
    fun createSession(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: CreateSessionRequest
    ): ResponseEntity<WorkoutSessionResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val session = sessionService.createSession(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session.toDecryptedResponse())
    }
    
    @PostMapping("/{sessionId}/end")
    fun endSession(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: Long
    ): ResponseEntity<WorkoutSessionResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val session = sessionService.endSession(sessionId, userId)
        return ResponseEntity.ok(session.toDecryptedResponse())
    }
    
    @GetMapping
    fun getUserSessions(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<WorkoutSessionResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        val sessions = sessionService.getUserSessions(userId, PageRequest.of(page, size))
        return ResponseEntity.ok(sessions.map { it.toDecryptedResponse() })
    }
    
    @GetMapping("/today")
    fun getTodaySessions(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<WorkoutSessionResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        val sessions = sessionService.getTodaySessions(userId)
        return ResponseEntity.ok(sessions.map { it.toDecryptedResponse() })
    }
    
    @GetMapping("/range")
    fun getSessionsInRange(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<List<WorkoutSessionResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        // Handle both date-only and datetime formats
        val start = try {
            LocalDateTime.parse(startDate)
        } catch (e: Exception) {
            LocalDate.parse(startDate).atStartOfDay()
        }
        val end = try {
            LocalDateTime.parse(endDate)
        } catch (e: Exception) {
            LocalDate.parse(endDate).atTime(23, 59, 59)
        }
        val sessions = sessionService.getUserSessionsInRange(userId, start, end)
        return ResponseEntity.ok(sessions.map { it.toDecryptedResponse() })
    }
    
    @GetMapping("/{sessionId}")
    fun getSession(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: Long
    ): ResponseEntity<WorkoutSessionDetailResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val session = sessionService.getSessionById(sessionId, userId)
        return ResponseEntity.ok(session.toDecryptedDetailResponse())
    }
    
    @DeleteMapping("/{sessionId}")
    fun deleteSession(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: Long
    ): ResponseEntity<Void> {
        val userId = jwt.getClaim<Long>("uid")
        sessionService.deleteSession(sessionId, userId)
        return ResponseEntity.noContent().build()
    }
    
    @PostMapping("/{sessionId}/exercises")
    fun addExercise(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: Long,
        @RequestBody request: AddExerciseRequest
    ): ResponseEntity<WorkoutExerciseResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val exercise = sessionService.addExerciseToSession(sessionId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(exercise.toDecryptedResponse())
    }
    
    @PostMapping("/exercises/{exerciseId}/sets")
    fun addSet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable exerciseId: Long,
        @RequestBody request: AddSetRequest
    ): ResponseEntity<WorkoutSetResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val set = sessionService.addSetToExercise(exerciseId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(set.toDecryptedResponse())
    }
    
    @PutMapping("/sets/{setId}")
    fun updateSet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable setId: Long,
        @RequestBody request: UpdateSetRequest
    ): ResponseEntity<WorkoutSetResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val set = sessionService.updateSet(setId, userId, request)
        return ResponseEntity.ok(set.toDecryptedResponse())
    }

    @DeleteMapping("/sets/{setId}")
    fun deleteSet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable setId: Long
    ): ResponseEntity<Void> {
        val userId = jwt.getClaim<Long>("uid")
        sessionService.deleteSet(setId, userId)
        return ResponseEntity.noContent().build()
    }
    @DeleteMapping("/exercises/{exerciseId}")
    fun deleteExercise(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable exerciseId: Long
    ): ResponseEntity<Void> {
        val userId = jwt.getClaim<Long>("uid")
        sessionService.deleteExerciseFromSession(exerciseId, userId)
        return ResponseEntity.noContent().build()
    }
    
    // Helper functions với giải mã
    private fun WorkoutSession.toDecryptedResponse() = WorkoutSessionResponse(
        id = id,
        name = encryptionHelper.decryptName(name),
        notes = encryptionHelper.decryptNullable(notes),
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = encryptionHelper.decryptIntNullable(durationMinutes),
        caloriesBurned = encryptionHelper.decryptIntNullable(caloriesBurned),
        exerciseCount = exercises.size
    )
    
    private fun WorkoutSession.toDecryptedDetailResponse() = WorkoutSessionDetailResponse(
        id = id,
        name = encryptionHelper.decryptName(name),
        notes = encryptionHelper.decryptNullable(notes),
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = encryptionHelper.decryptIntNullable(durationMinutes),
        caloriesBurned = encryptionHelper.decryptIntNullable(caloriesBurned),
        exercises = exercises.sortedBy { encryptionHelper.decryptInt(it.exerciseOrder) }.map { it.toDecryptedResponse() }
    )
    
    private fun WorkoutExercise.toDecryptedResponse() = WorkoutExerciseResponse(
        id = id,
        exerciseName = encryptionHelper.decryptName(exerciseName),
        lessonId = lesson?.id,
        exerciseOrder = encryptionHelper.decryptInt(exerciseOrder),
        notes = encryptionHelper.decryptNullable(notes),
        sets = sets.sortedBy { encryptionHelper.decryptInt(it.setNumber) }.map { it.toDecryptedResponse() }
    )
    
    private fun WorkoutSet.toDecryptedResponse() = WorkoutSetResponse(
        id = id,
        setNumber = encryptionHelper.decryptInt(setNumber),
        reps = encryptionHelper.decryptInt(reps),
        weightKg = encryptionHelper.decryptDoubleNullable(weightKg),
        durationSeconds = encryptionHelper.decryptIntNullable(durationSeconds),
        isWarmup = isWarmup,
        isCompleted = isCompleted,
        notes = encryptionHelper.decryptNullable(notes)
    )
}

// Response DTOs
data class WorkoutSessionResponse(
    val id: Long,
    val name: String,
    val notes: String?,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val durationMinutes: Int?,
    val caloriesBurned: Int?,
    val exerciseCount: Int
)

data class WorkoutSessionDetailResponse(
    val id: Long,
    val name: String,
    val notes: String?,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val durationMinutes: Int?,
    val caloriesBurned: Int?,
    val exercises: List<WorkoutExerciseResponse>
)

data class WorkoutExerciseResponse(
    val id: Long,
    val exerciseName: String,
    val lessonId: Long?,
    val exerciseOrder: Int,
    val notes: String?,
    val sets: List<WorkoutSetResponse>
)

data class WorkoutSetResponse(
    val id: Long,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double?,
    val durationSeconds: Int?,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val notes: String?
)


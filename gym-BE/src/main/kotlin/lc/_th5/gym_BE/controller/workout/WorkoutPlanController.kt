package lc._th5.gym_BE.controller.workout

import lc._th5.gym_BE.model.workout.*
import lc._th5.gym_BE.service.workout.*
import lc._th5.gym_BE.util.WorkoutEncryptionHelper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workout/plans")
class WorkoutPlanController(
    private val planService: WorkoutPlanService,
    private val encryptionHelper: WorkoutEncryptionHelper
) {
    
    @PostMapping
    fun createPlan(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: CreatePlanRequest
    ): ResponseEntity<WorkoutPlanResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val plan = planService.createPlan(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(plan.toDecryptedResponse())
    }
    
    @GetMapping
    fun getUserPlans(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<WorkoutPlanResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        val plans = planService.getUserPlans(userId)
        return ResponseEntity.ok(plans.map { it.toDecryptedResponse() })
    }
    
    @GetMapping("/active")
    fun getActivePlan(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<WorkoutPlanDetailResponse?> {
        val userId = jwt.getClaim<Long>("uid")
        val plan = planService.getActivePlan(userId)
        return ResponseEntity.ok(plan?.toDecryptedDetailResponse())
    }
    
    @GetMapping("/today")
    fun getTodayPlan(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<WorkoutPlanDayResponse?> {
        val userId = jwt.getClaim<Long>("uid")
        val day = planService.getTodayPlan(userId)
        return ResponseEntity.ok(day?.toDecryptedResponse())
    }
    
    @GetMapping("/{planId}")
    fun getPlan(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable planId: Long
    ): ResponseEntity<WorkoutPlanDetailResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val plan = planService.getPlanById(planId, userId)
        return ResponseEntity.ok(plan.toDecryptedDetailResponse())
    }
    
    @GetMapping("/{planId}/days")
    fun getPlanDays(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable planId: Long
    ): ResponseEntity<List<WorkoutPlanDayResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        val days = planService.getPlanDays(planId, userId)
        return ResponseEntity.ok(days.map { it.toDecryptedResponse() })
    }
    
    @PostMapping("/activate")
    fun setActivePlan(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: SetActivePlanRequest
    ): ResponseEntity<WorkoutPlanResponse?> {
        val userId = jwt.getClaim<Long>("uid")
        val plan = planService.setActivePlan(request.planId, userId)
        return ResponseEntity.ok(plan?.toDecryptedResponse())
    }
    
    @PostMapping("/{planId}/days")
    fun addDay(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable planId: Long,
        @RequestBody request: AddPlanDayRequest
    ): ResponseEntity<WorkoutPlanDayResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val day = planService.addDayToPlan(planId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(day.toDecryptedResponse())
    }
    
    @PostMapping("/days/{dayId}/exercises")
    fun addExercise(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable dayId: Long,
        @RequestBody request: AddPlanExerciseRequest
    ): ResponseEntity<WorkoutPlanExerciseResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val exercise = planService.addExerciseToPlanDay(dayId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(exercise.toDecryptedResponse())
    }
    
    @DeleteMapping("/{planId}")
    fun deletePlan(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable planId: Long
    ): ResponseEntity<Void> {
        val userId = jwt.getClaim<Long>("uid")
        planService.deletePlan(planId, userId)
        return ResponseEntity.noContent().build()
    }
    
    @DeleteMapping("/days/{dayId}")
    fun deleteDay(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable dayId: Long
    ): ResponseEntity<Void> {
        val userId = jwt.getClaim<Long>("uid")
        planService.deletePlanDay(dayId, userId)
        return ResponseEntity.noContent().build()
    }
    
    @PutMapping("/exercises/{exerciseId}")
    fun updateExercise(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable exerciseId: Long,
        @RequestBody request: UpdatePlanExerciseRequest
    ): ResponseEntity<WorkoutPlanExerciseResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val exercise = planService.updatePlanExercise(exerciseId, userId, request)
        return ResponseEntity.ok(exercise.toDecryptedResponse())
    }
    
    @DeleteMapping("/exercises/{exerciseId}")
    fun deleteExercise(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable exerciseId: Long
    ): ResponseEntity<Void> {
        val userId = jwt.getClaim<Long>("uid")
        planService.deletePlanExercise(exerciseId, userId)
        return ResponseEntity.noContent().build()
    }
    
    // Helper functions với giải mã
    private fun WorkoutPlan.toDecryptedResponse() = WorkoutPlanResponse(
        id = id,
        name = encryptionHelper.decryptName(name),
        description = encryptionHelper.decryptNullable(description),
        isActive = isActive,
        weeksDuration = weeksDuration,
        daysCount = days.size
    )
    
    private fun WorkoutPlan.toDecryptedDetailResponse() = WorkoutPlanDetailResponse(
        id = id,
        name = encryptionHelper.decryptName(name),
        description = encryptionHelper.decryptNullable(description),
        isActive = isActive,
        weeksDuration = weeksDuration,
        days = days.sortedBy { it.dayOfWeek }.map { it.toDecryptedResponse() }
    )
    
    private fun WorkoutPlanDay.toDecryptedResponse() = WorkoutPlanDayResponse(
        id = id,
        dayOfWeek = dayOfWeek,
        name = encryptionHelper.decryptName(name),
        isRestDay = isRestDay,
        exercises = exercises.sortedBy { encryptionHelper.decryptInt(it.exerciseOrder) }.map { it.toDecryptedResponse() }
    )
    
    private fun WorkoutPlanExercise.toDecryptedResponse() = WorkoutPlanExerciseResponse(
        id = id,
        exerciseName = encryptionHelper.decryptName(exerciseName),
        lessonId = lesson?.id,
        exerciseOrder = encryptionHelper.decryptInt(exerciseOrder),
        targetSets = encryptionHelper.decryptInt(targetSets),
        targetReps = encryptionHelper.decryptName(targetReps),
        targetWeightKg = encryptionHelper.decryptDoubleNullable(targetWeightKg),
        restSeconds = encryptionHelper.decryptInt(restSeconds),
        notes = encryptionHelper.decryptNullable(notes)
    )
}

// Response DTOs
data class WorkoutPlanResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val weeksDuration: Int?,
    val daysCount: Int
)

data class WorkoutPlanDetailResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val weeksDuration: Int?,
    val days: List<WorkoutPlanDayResponse>
)

data class WorkoutPlanDayResponse(
    val id: Long,
    val dayOfWeek: DayOfWeek,
    val name: String,
    val isRestDay: Boolean,
    val exercises: List<WorkoutPlanExerciseResponse>
)

data class WorkoutPlanExerciseResponse(
    val id: Long,
    val exerciseName: String,
    val lessonId: Long?,
    val exerciseOrder: Int,
    val targetSets: Int,
    val targetReps: String,
    val targetWeightKg: Double?,
    val restSeconds: Int,
    val notes: String?
)



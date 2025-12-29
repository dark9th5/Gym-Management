package lc._th5.gym_BE.controller.workout

import lc._th5.gym_BE.model.workout.*
import lc._th5.gym_BE.service.workout.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workout/plans")
class WorkoutPlanController(
    private val planService: WorkoutPlanService
) {
    
    @PostMapping
    fun createPlan(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: CreatePlanRequest
    ): ResponseEntity<WorkoutPlanResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val plan = planService.createPlan(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(plan.toResponse())
    }
    
    @GetMapping
    fun getUserPlans(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<WorkoutPlanResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        val plans = planService.getUserPlans(userId)
        return ResponseEntity.ok(plans.map { it.toResponse() })
    }
    
    @GetMapping("/active")
    fun getActivePlan(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<WorkoutPlanDetailResponse?> {
        val userId = jwt.getClaim<Long>("uid")
        val plan = planService.getActivePlan(userId)
        return ResponseEntity.ok(plan?.toDetailResponse())
    }
    
    @GetMapping("/today")
    fun getTodayPlan(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<WorkoutPlanDayResponse?> {
        val userId = jwt.getClaim<Long>("uid")
        val day = planService.getTodayPlan(userId)
        return ResponseEntity.ok(day?.toResponse())
    }
    
    @GetMapping("/{planId}")
    fun getPlan(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable planId: Long
    ): ResponseEntity<WorkoutPlanDetailResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val plan = planService.getPlanById(planId, userId)
        return ResponseEntity.ok(plan.toDetailResponse())
    }
    
    @GetMapping("/{planId}/days")
    fun getPlanDays(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable planId: Long
    ): ResponseEntity<List<WorkoutPlanDayResponse>> {
        val userId = jwt.getClaim<Long>("uid")
        val days = planService.getPlanDays(planId, userId)
        return ResponseEntity.ok(days.map { it.toResponse() })
    }
    
    @PostMapping("/activate")
    fun setActivePlan(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: SetActivePlanRequest
    ): ResponseEntity<WorkoutPlanResponse?> {
        val userId = jwt.getClaim<Long>("uid")
        val plan = planService.setActivePlan(request.planId, userId)
        return ResponseEntity.ok(plan?.toResponse())
    }
    
    @PostMapping("/{planId}/days")
    fun addDay(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable planId: Long,
        @RequestBody request: AddPlanDayRequest
    ): ResponseEntity<WorkoutPlanDayResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val day = planService.addDayToPlan(planId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(day.toResponse())
    }
    
    @PostMapping("/days/{dayId}/exercises")
    fun addExercise(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable dayId: Long,
        @RequestBody request: AddPlanExerciseRequest
    ): ResponseEntity<WorkoutPlanExerciseResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val exercise = planService.addExerciseToPlanDay(dayId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(exercise.toResponse())
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
        return ResponseEntity.ok(exercise.toResponse())
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

// Extension functions
fun WorkoutPlan.toResponse() = WorkoutPlanResponse(
    id = id,
    name = name,
    description = description,
    isActive = isActive,
    weeksDuration = weeksDuration,
    daysCount = days.size
)

fun WorkoutPlan.toDetailResponse() = WorkoutPlanDetailResponse(
    id = id,
    name = name,
    description = description,
    isActive = isActive,
    weeksDuration = weeksDuration,
    days = days.sortedBy { it.dayOfWeek }.map { it.toResponse() }
)

fun WorkoutPlanDay.toResponse() = WorkoutPlanDayResponse(
    id = id,
    dayOfWeek = dayOfWeek,
    name = name,
    isRestDay = isRestDay,
    exercises = exercises.sortedBy { it.exerciseOrder }.map { it.toResponse() }
)

fun WorkoutPlanExercise.toResponse() = WorkoutPlanExerciseResponse(
    id = id,
    exerciseName = exerciseName,
    lessonId = lesson?.id,
    exerciseOrder = exerciseOrder,
    targetSets = targetSets,
    targetReps = targetReps,
    targetWeightKg = targetWeightKg,
    restSeconds = restSeconds,
    notes = notes
)

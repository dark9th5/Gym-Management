package lc._th5.gym_BE.service.workout

import lc._th5.gym_BE.model.workout.*
import lc._th5.gym_BE.repository.UserRepository
import lc._th5.gym_BE.repository.workout.*
import lc._th5.gym_BE.repository.guidance.GuidanceLessonRepository
import lc._th5.gym_BE.util.WorkoutEncryptionHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WorkoutPlanService(
    private val planRepository: WorkoutPlanRepository,
    private val planDayRepository: WorkoutPlanDayRepository,
    private val planExerciseRepository: WorkoutPlanExerciseRepository,
    private val userRepository: UserRepository,
    private val lessonRepository: GuidanceLessonRepository,
    private val encryptionHelper: WorkoutEncryptionHelper
) {
    
    @Transactional
    fun createPlan(userId: Long, request: CreatePlanRequest): WorkoutPlan {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        if (planRepository.existsByUserIdAndName(userId, request.name)) {
            throw IllegalArgumentException("Plan with this name already exists")
        }
        
        // Deactivate other plans if this one is active
        if (request.isActive) {
            planRepository.findByUserIdAndIsActiveTrue(userId)?.let {
                it.isActive = false
                planRepository.save(it)
            }
        }
        
        // Mã hóa dữ liệu kế hoạch
        val plan = WorkoutPlan(
            user = user,
            name = encryptionHelper.encryptName(request.name),
            description = encryptionHelper.encryptNullable(request.description),
            isActive = request.isActive,
            weeksDuration = request.weeksDuration
        )
        
        return planRepository.save(plan)
    }
    
    @Transactional
    fun addDayToPlan(planId: Long, userId: Long, request: AddPlanDayRequest): WorkoutPlanDay {
        val plan = getPlanByIdAndUser(planId, userId)
        
        // Check if day already exists
        planDayRepository.findByPlanIdAndDayOfWeek(planId, request.dayOfWeek)?.let {
            throw IllegalArgumentException("Day already exists in plan")
        }
        
        // Mã hóa tên ngày tập
        val day = WorkoutPlanDay(
            plan = plan,
            dayOfWeek = request.dayOfWeek,
            name = encryptionHelper.encryptName(request.name),
            isRestDay = request.isRestDay
        )
        
        return planDayRepository.save(day)
    }
    
    @Transactional
    fun addExerciseToPlanDay(dayId: Long, userId: Long, request: AddPlanExerciseRequest): WorkoutPlanExercise {
        val day = planDayRepository.findById(dayId)
            .orElseThrow { IllegalArgumentException("Day not found") }
        
        if (day.plan.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        val lesson = request.lessonId?.let {
            lessonRepository.findById(it).orElse(null)
        }
        
        val exerciseOrder = day.exercises.size + 1
        
        // Mã hóa tất cả dữ liệu bài tập trong kế hoạch
        val exercise = WorkoutPlanExercise(
            planDay = day,
            lesson = lesson,
            exerciseName = encryptionHelper.encryptName(request.exerciseName),
            exerciseOrder = encryptionHelper.encryptInt(exerciseOrder),
            targetSets = encryptionHelper.encryptInt(request.targetSets),
            targetReps = encryptionHelper.encryptName(request.targetReps),
            targetWeightKg = encryptionHelper.encryptDoubleNullable(request.targetWeightKg),
            restSeconds = encryptionHelper.encryptInt(request.restSeconds),
            notes = encryptionHelper.encryptNullable(request.notes)
        )
        
        return planExerciseRepository.save(exercise)
    }
    
    fun getUserPlans(userId: Long): List<WorkoutPlan> {
        return planRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }
    
    fun getActivePlan(userId: Long): WorkoutPlan? {
        return planRepository.findByUserIdAndIsActiveTrue(userId)
    }
    
    fun getPlanById(planId: Long, userId: Long): WorkoutPlan {
        return getPlanByIdAndUser(planId, userId)
    }
    
    fun getPlanDays(planId: Long, userId: Long): List<WorkoutPlanDay> {
        val plan = getPlanByIdAndUser(planId, userId)
        return planDayRepository.findByPlanIdOrderByDayOfWeek(plan.id)
    }
    
    fun getTodayPlan(userId: Long): WorkoutPlanDay? {
        val activePlan = getActivePlan(userId) ?: return null
        val today = java.time.LocalDate.now().dayOfWeek
        val dayOfWeek = DayOfWeek.valueOf(today.name)
        return planDayRepository.findByPlanIdAndDayOfWeek(activePlan.id, dayOfWeek)
    }
    
    @Transactional
    fun setActivePlan(planId: Long?, userId: Long): WorkoutPlan? {
        if (planId == null) {
            // Deactivate all plans
            planRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach {
                if (it.isActive) {
                    it.isActive = false
                    planRepository.save(it)
                }
            }
            return null
        }
        
        val plan = getPlanByIdAndUser(planId, userId)
        
        // Deactivate all other plans
        planRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach {
            if (it.id != planId && it.isActive) {
                it.isActive = false
                planRepository.save(it)
            }
        }
        
        plan.isActive = true
        return planRepository.save(plan)
    }
    
    @Transactional
    fun deletePlan(planId: Long, userId: Long) {
        val plan = getPlanByIdAndUser(planId, userId)
        planRepository.delete(plan)
    }
    
    @Transactional
    fun deletePlanDay(dayId: Long, userId: Long) {
        val day = planDayRepository.findById(dayId)
            .orElseThrow { IllegalArgumentException("Day not found") }
        
        if (day.plan.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        planDayRepository.delete(day)
    }
    
    @Transactional
    fun updatePlanExercise(exerciseId: Long, userId: Long, request: UpdatePlanExerciseRequest): WorkoutPlanExercise {
        val exercise = planExerciseRepository.findById(exerciseId)
            .orElseThrow { IllegalArgumentException("Exercise not found") }
        
        if (exercise.planDay.plan.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        // Update lesson if provided
        val lesson = request.lessonId?.let {
            lessonRepository.findById(it).orElse(null)
        }
        
        // Mã hóa tất cả các trường nếu có cập nhật
        exercise.exerciseName = request.exerciseName?.let { encryptionHelper.encryptName(it) } ?: exercise.exerciseName
        exercise.lesson = lesson ?: exercise.lesson
        exercise.targetSets = request.targetSets?.let { encryptionHelper.encryptInt(it) } ?: exercise.targetSets
        exercise.targetReps = request.targetReps?.let { encryptionHelper.encryptName(it) } ?: exercise.targetReps
        exercise.targetWeightKg = request.targetWeightKg?.let { encryptionHelper.encryptDouble(it) } ?: exercise.targetWeightKg
        exercise.restSeconds = request.restSeconds?.let { encryptionHelper.encryptInt(it) } ?: exercise.restSeconds
        exercise.notes = request.notes?.let { encryptionHelper.encryptNullable(it) } ?: exercise.notes
        
        return planExerciseRepository.save(exercise)
    }
    
    @Transactional
    fun deletePlanExercise(exerciseId: Long, userId: Long) {
        val exercise = planExerciseRepository.findById(exerciseId)
            .orElseThrow { IllegalArgumentException("Exercise not found") }
        
        if (exercise.planDay.plan.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        planExerciseRepository.delete(exercise)
    }
    
    private fun getPlanByIdAndUser(planId: Long, userId: Long): WorkoutPlan {
        val plan = planRepository.findById(planId)
            .orElseThrow { IllegalArgumentException("Plan not found") }
        
        if (plan.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        return plan
    }
}

// Request DTOs
data class CreatePlanRequest(
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val weeksDuration: Int? = null
)

data class AddPlanDayRequest(
    val dayOfWeek: DayOfWeek,
    val name: String,
    val isRestDay: Boolean = false
)

data class AddPlanExerciseRequest(
    val exerciseName: String,
    val lessonId: Long? = null,
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val targetWeightKg: Double? = null,
    val restSeconds: Int = 60,
    val notes: String? = null
)

data class UpdatePlanExerciseRequest(
    val exerciseName: String? = null,
    val lessonId: Long? = null,
    val targetSets: Int? = null,
    val targetReps: String? = null,
    val targetWeightKg: Double? = null,
    val restSeconds: Int? = null,
    val notes: String? = null
)

data class SetActivePlanRequest(
    val planId: Long? = null
)

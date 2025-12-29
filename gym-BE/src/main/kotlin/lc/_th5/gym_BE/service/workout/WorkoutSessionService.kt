package lc._th5.gym_BE.service.workout

import lc._th5.gym_BE.model.workout.*
import lc._th5.gym_BE.repository.UserRepository
import lc._th5.gym_BE.repository.workout.*
import lc._th5.gym_BE.repository.guidance.GuidanceLessonRepository
import lc._th5.gym_BE.util.WorkoutEncryptionHelper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WorkoutSessionService(
    private val sessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: WorkoutExerciseRepository,
    private val setRepository: WorkoutSetRepository,
    private val userRepository: UserRepository,
    private val lessonRepository: GuidanceLessonRepository,
    private val streakService: StreakService,
    private val encryptionHelper: WorkoutEncryptionHelper
) {
    
    @Transactional
    fun createSession(userId: Long, request: CreateSessionRequest): WorkoutSession {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        // Mã hóa dữ liệu trước khi lưu
        val session = WorkoutSession(
            user = user,
            name = encryptionHelper.encryptName(request.name),
            notes = encryptionHelper.encryptNullable(request.notes),
            startedAt = request.startedAt ?: LocalDateTime.now()
        )
        
        return sessionRepository.save(session)
    }
    
    @Transactional
    fun endSession(sessionId: Long, userId: Long): WorkoutSession {
        val session = getSessionByIdAndUser(sessionId, userId)
        
        session.endedAt = LocalDateTime.now()
        session.durationMinutes = java.time.Duration.between(session.startedAt, session.endedAt).toMinutes().toInt()
        
        val savedSession = sessionRepository.save(session)
        
        // Cập nhật streak
        streakService.updateStreak(userId)
        
        return savedSession
    }
    
    @Transactional
    fun addExerciseToSession(sessionId: Long, userId: Long, request: AddExerciseRequest): WorkoutExercise {
        val session = getSessionByIdAndUser(sessionId, userId)
        
        val lesson = request.lessonId?.let { 
            lessonRepository.findById(it).orElse(null) 
        }
        
        val exerciseOrder = session.exercises.size + 1
        
        // Mã hóa dữ liệu bài tập
        val exercise = WorkoutExercise(
            session = session,
            lesson = lesson,
            exerciseName = encryptionHelper.encryptName(request.exerciseName),
            exerciseOrder = exerciseOrder,
            notes = encryptionHelper.encryptNullable(request.notes)
        )
        
        return exerciseRepository.save(exercise)
    }
    
    @Transactional
    fun addSetToExercise(exerciseId: Long, userId: Long, request: AddSetRequest): WorkoutSet {
        val exercise = exerciseRepository.findById(exerciseId)
            .orElseThrow { IllegalArgumentException("Exercise not found") }
        
        if (exercise.session.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        val setNumber = exercise.sets.size + 1
        
        // Mã hóa notes của set
        val set = WorkoutSet(
            exercise = exercise,
            setNumber = setNumber,
            reps = request.reps,
            weightKg = request.weightKg,
            durationSeconds = request.durationSeconds,
            isWarmup = request.isWarmup,
            isCompleted = request.isCompleted,
            notes = encryptionHelper.encryptNullable(request.notes)
        )
        
        return setRepository.save(set)
    }
    
    fun getSessionById(sessionId: Long, userId: Long): WorkoutSession {
        return getSessionByIdAndUser(sessionId, userId)
    }
    
    fun getUserSessions(userId: Long, pageable: Pageable): Page<WorkoutSession> {
        return sessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable)
    }
    
    fun getUserSessionsInRange(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<WorkoutSession> {
        return sessionRepository.findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(userId, startDate, endDate)
    }
    
    fun getTodaySessions(userId: Long): List<WorkoutSession> {
        return sessionRepository.findTodaySessionsByUserId(userId)
    }
    
    @Transactional
    fun deleteSession(sessionId: Long, userId: Long) {
        val session = getSessionByIdAndUser(sessionId, userId)
        sessionRepository.delete(session)
    }
    
    @Transactional
    fun updateSet(setId: Long, userId: Long, request: UpdateSetRequest): WorkoutSet {
        val set = setRepository.findById(setId)
            .orElseThrow { IllegalArgumentException("Set not found") }
        
        if (set.exercise.session.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        // Mã hóa notes nếu có cập nhật
        val updatedSet = set.copy(
            reps = request.reps ?: set.reps,
            weightKg = request.weightKg ?: set.weightKg,
            durationSeconds = request.durationSeconds ?: set.durationSeconds,
            isCompleted = request.isCompleted ?: set.isCompleted,
            notes = request.notes?.let { encryptionHelper.encryptNullable(it) } ?: set.notes
        )
        
        return setRepository.save(updatedSet)
    }

    @Transactional
    fun deleteSet(setId: Long, userId: Long) {
        val set = setRepository.findById(setId)
            .orElseThrow { IllegalArgumentException("Set not found") }
        
        if (set.exercise.session.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        setRepository.delete(set)
    }
    
    private fun getSessionByIdAndUser(sessionId: Long, userId: Long): WorkoutSession {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        if (session.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        return session
    }
    @Transactional
    fun deleteExerciseFromSession(exerciseId: Long, userId: Long) {
        val exercise = exerciseRepository.findById(exerciseId)
            .orElseThrow { IllegalArgumentException("Exercise not found") }
        
        if (exercise.session.user.id != userId) {
            throw IllegalArgumentException("Not authorized")
        }
        
        exerciseRepository.delete(exercise)
    }
}

// Request DTOs
data class CreateSessionRequest(
    val name: String,
    val notes: String? = null,
    val startedAt: LocalDateTime? = null
)

data class AddExerciseRequest(
    val exerciseName: String,
    val lessonId: Long? = null,
    val notes: String? = null
)

data class AddSetRequest(
    val reps: Int,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val isWarmup: Boolean = false,
    val isCompleted: Boolean = true,
    val notes: String? = null
)

data class UpdateSetRequest(
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val isCompleted: Boolean? = null,
    val notes: String? = null
)

package lc._th5.gym_BE.service.workout

import lc._th5.gym_BE.repository.workout.WorkoutSessionRepository
import lc._th5.gym_BE.repository.workout.WorkoutSetRepository
import lc._th5.gym_BE.repository.workout.WorkoutExerciseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class StatisticsService(
    private val sessionRepository: WorkoutSessionRepository,
    private val setRepository: WorkoutSetRepository,
    private val exerciseRepository: WorkoutExerciseRepository
) {
    
    fun getOverviewStatistics(userId: Long, days: Int = 30): OverviewStatistics {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        
        return try {
            val sessions = sessionRepository.findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(
                userId, startDate, LocalDateTime.now()
            )
            
            val totalSessions = sessions.size
            val totalDuration = sessionRepository.getTotalDurationMinutes(userId, startDate) ?: 0
            val totalReps = setRepository.getTotalReps(userId, startDate) ?: 0L
            val totalVolume = setRepository.getTotalVolume(userId, startDate) ?: 0.0
            
            // Calculate additional stats using queries to avoid lazy loading issues
            val totalExercises = exerciseRepository.getTotalExerciseCount(userId, startDate) ?: 0L
            val totalSets = setRepository.getTotalSetCount(userId, startDate) ?: 0L
            val avgDuration = if (totalSessions > 0) totalDuration / totalSessions else 0
            
            OverviewStatistics(
                periodDays = days,
                totalSessions = totalSessions,
                totalWorkouts = totalSessions, // Same as sessions
                totalMinutes = totalDuration,
                totalCalories = (totalDuration * 5), // Estimate: 5 cal per minute
                totalExercises = totalExercises.toInt(),
                totalSets = totalSets.toInt(),
                totalDurationMinutes = totalDuration,
                totalReps = totalReps,
                totalVolumeKg = totalVolume,
                averageSessionDuration = avgDuration,
                avgDurationMinutes = avgDuration,
                sessionsPerWeek = (totalSessions * 7.0 / days).let { "%.1f".format(it).toDouble() }
            )
        } catch (e: Exception) {
            // Return empty statistics on error
            OverviewStatistics(
                periodDays = days,
                totalSessions = 0,
                totalWorkouts = 0,
                totalMinutes = 0,
                totalCalories = 0,
                totalExercises = 0,
                totalSets = 0,
                totalDurationMinutes = 0,
                totalReps = 0L,
                totalVolumeKg = 0.0,
                averageSessionDuration = 0,
                avgDurationMinutes = 0,
                sessionsPerWeek = 0.0
            )
        }
    }
    
    fun getWorkoutHistory(userId: Long, days: Int = 30): List<DailyWorkoutSummary> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        val workoutCounts = sessionRepository.getWorkoutCountByDate(userId, startDate)
        
        return workoutCounts.map { row ->
            DailyWorkoutSummary(
                date = (row[0] as java.sql.Date).toLocalDate(),
                sessionCount = (row[1] as Long).toInt()
            )
        }
    }
    
    fun getExerciseStatistics(userId: Long, exerciseName: String): ExerciseStatistics {
        val maxWeight = setRepository.getMaxWeightForExercise(userId, exerciseName)
        
        // Get weight history
        val historyData = setRepository.getWeightHistoryForExercise(userId, exerciseName)
        val weightHistory = historyData.take(30).map { row ->
            WeightHistoryEntry(
                date = (row[0] as java.sql.Date).toLocalDate(),
                maxWeightKg = (row[1] as Number).toDouble(),
                avgWeightKg = (row[2] as Number?)?.toDouble(),
                totalSets = (row[3] as Long).toInt(),
                totalReps = (row[4] as Long).toInt()
            )
        }.reversed() // Oldest first for chart
        
        return ExerciseStatistics(
            exerciseName = exerciseName,
            maxWeightKg = maxWeight,
            totalSessions = weightHistory.size,
            totalSets = weightHistory.sumOf { it.totalSets },
            weightHistory = weightHistory
        )
    }
    
    fun getMostFrequentExercises(userId: Long, limit: Int = 10): List<ExerciseFrequency> {
        val frequentExercises = exerciseRepository.getMostFrequentExercises(userId)
        
        return frequentExercises.take(limit).map { row ->
            ExerciseFrequency(
                exerciseName = row[0] as String,
                count = (row[1] as Long).toInt()
            )
        }
    }
    
    fun getWeeklyProgress(userId: Long, weeks: Int = 4): List<WeeklyProgress> {
        val result = mutableListOf<WeeklyProgress>()
        val now = LocalDateTime.now()
        
        for (i in 0 until weeks) {
            val weekStart = now.minusWeeks(i.toLong()).with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay()
            val weekEnd = weekStart.plusDays(7)
            
            val sessions = sessionRepository.findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(
                userId, weekStart, weekEnd
            )
            
            val totalDuration = sessions.sumOf { it.durationMinutes ?: 0 }
            val totalVolume = setRepository.getTotalVolume(userId, weekStart) ?: 0.0
            
            result.add(WeeklyProgress(
                weekStartDate = weekStart.toLocalDate(),
                sessionCount = sessions.size,
                totalDurationMinutes = totalDuration,
                totalVolumeKg = totalVolume
            ))
        }
        
        return result.reversed() // Trả về từ tuần cũ đến mới
    }
    
    fun getMonthlyCalendar(userId: Long, year: Int, month: Int): List<CalendarDay> {
        val startDate = LocalDate.of(year, month, 1).atStartOfDay()
        val endDate = startDate.plusMonths(1)
        
        val sessions = sessionRepository.findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(
            userId, startDate, endDate
        )
        
        val workoutDates = sessions.groupBy { it.startedAt.toLocalDate() }
        
        val daysInMonth = startDate.toLocalDate().lengthOfMonth()
        
        return (1..daysInMonth).map { day ->
            val date = LocalDate.of(year, month, day)
            val daySessions = workoutDates[date] ?: emptyList()
            
            CalendarDay(
                date = date,
                hasWorkout = daySessions.isNotEmpty(),
                sessionCount = daySessions.size,
                totalDurationMinutes = daySessions.sumOf { it.durationMinutes ?: 0 }
            )
        }
    }
}

// Response DTOs
data class OverviewStatistics(
    val periodDays: Int,
    val totalSessions: Int,
    val totalWorkouts: Int = 0,
    val totalMinutes: Int = 0,
    val totalCalories: Int = 0,
    val totalExercises: Int = 0,
    val totalSets: Int = 0,
    val totalDurationMinutes: Int,
    val totalReps: Long,
    val totalVolumeKg: Double,
    val averageSessionDuration: Int,
    val avgDurationMinutes: Int = 0,
    val sessionsPerWeek: Double
)

data class DailyWorkoutSummary(
    val date: LocalDate,
    val sessionCount: Int
)

data class ExerciseStatistics(
    val exerciseName: String,
    val maxWeightKg: Double?,
    val totalSessions: Int = 0,
    val totalSets: Int = 0,
    val weightHistory: List<WeightHistoryEntry> = emptyList()
)

data class WeightHistoryEntry(
    val date: LocalDate,
    val maxWeightKg: Double,
    val avgWeightKg: Double? = null,
    val totalSets: Int = 0,
    val totalReps: Int = 0
)

data class ExerciseFrequency(
    val exerciseName: String,
    val count: Int
)

data class WeeklyProgress(
    val weekStartDate: LocalDate,
    val sessionCount: Int,
    val totalDurationMinutes: Int,
    val totalVolumeKg: Double
)

data class CalendarDay(
    val date: LocalDate,
    val hasWorkout: Boolean,
    val sessionCount: Int,
    val totalDurationMinutes: Int
)

package com.lc9th5.gym.data.model

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ===== UI OPTIMIZATION: @Immutable annotations help Compose skip unnecessary recompositions =====
// When a data class is marked @Immutable, Compose knows it won't change after creation,
// allowing it to skip recomposition of composables that use unchanged instances.

// ==================== Workout Session Models ====================

@Immutable
data class WorkoutSession(
    val id: Long,
    val name: String,
    val notes: String? = null,
    @SerializedName("startedAt") val startedAt: String,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null,
    @SerializedName("caloriesBurned") val caloriesBurned: Int? = null,
    @SerializedName("exerciseCount") val exerciseCount: Int = 0
)

data class WorkoutSessionDetail(
    val id: Long,
    val name: String,
    val notes: String? = null,
    @SerializedName("startedAt") val startedAt: String,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null,
    @SerializedName("caloriesBurned") val caloriesBurned: Int? = null,
    val exercises: List<WorkoutExerciseDetail> = emptyList()
)

data class WorkoutExercise(
    val id: Long,
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("lessonId") val lessonId: Long? = null,
    @SerializedName("exerciseOrder") val exerciseOrder: Int = 0,
    val notes: String? = null,
    val sets: List<WorkoutSet> = emptyList()
)

data class WorkoutSet(
    val id: Long,
    @SerializedName("setNumber") val setNumber: Int,
    val reps: Int,
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("isWarmup") val isWarmup: Boolean = false,
    @SerializedName("isCompleted") val isCompleted: Boolean = true,
    val notes: String? = null
)

// Request DTOs
data class CreateSessionRequest(
    val name: String,
    val notes: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null
)

data class AddExerciseRequest(
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("lessonId") val lessonId: Long? = null,
    val notes: String? = null
)

data class AddSetRequest(
    val reps: Int,
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("isWarmup") val isWarmup: Boolean = false,
    @SerializedName("isCompleted") val isCompleted: Boolean = true,
    val notes: String? = null
)

data class UpdateSetRequest(
    val reps: Int? = null,
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("isCompleted") val isCompleted: Boolean? = null,
    val notes: String? = null
)

// ==================== Workout Plan Models ====================

@Immutable
data class WorkoutPlan(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerializedName("isActive") val isActive: Boolean = false,
    @SerializedName("weeksDuration") val weeksDuration: Int? = null,
    @SerializedName("daysCount") val daysCount: Int = 0
)

data class WorkoutPlanDetail(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerializedName("isActive") val isActive: Boolean = false,
    @SerializedName("weeksDuration") val weeksDuration: Int? = null,
    val days: List<WorkoutPlanDayDetail> = emptyList()
)

@Immutable
data class WorkoutPlanDay(
    val id: Long,
    @SerializedName("dayOfWeek") val dayOfWeek: DayOfWeek,
    val name: String,
    @SerializedName("isRestDay") val isRestDay: Boolean = false,
    val exercises: List<WorkoutPlanExercise> = emptyList()
)

@Immutable
data class WorkoutPlanExercise(
    val id: Long,
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("lessonId") val lessonId: Long? = null,
    @SerializedName("exerciseOrder") val exerciseOrder: Int = 0,
    @SerializedName("targetSets") val targetSets: Int = 3,
    @SerializedName("targetReps") val targetReps: String = "8-12",
    @SerializedName("targetWeightKg") val targetWeightKg: Double? = null,
    @SerializedName("restSeconds") val restSeconds: Int = 60,
    val notes: String? = null
)

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

// Request DTOs
data class CreatePlanRequest(
    val name: String,
    val description: String? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("weeksDuration") val weeksDuration: Int? = null
)

data class AddPlanDayRequest(
    @SerializedName("dayOfWeek") val dayOfWeek: DayOfWeek,
    val name: String,
    @SerializedName("isRestDay") val isRestDay: Boolean = false
)

data class AddPlanExerciseRequest(
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("lessonId") val lessonId: Long? = null,
    @SerializedName("targetSets") val targetSets: Int = 3,
    @SerializedName("targetReps") val targetReps: String = "8-12",
    @SerializedName("targetWeightKg") val targetWeightKg: Double? = null,
    @SerializedName("restSeconds") val restSeconds: Int = 60,
    val notes: String? = null
)

data class UpdatePlanExerciseRequest(
    @SerializedName("exerciseName") val exerciseName: String? = null,
    @SerializedName("lessonId") val lessonId: Long? = null,
    @SerializedName("targetSets") val targetSets: Int? = null,
    @SerializedName("targetReps") val targetReps: String? = null,
    @SerializedName("targetWeightKg") val targetWeightKg: Double? = null,
    @SerializedName("restSeconds") val restSeconds: Int? = null,
    val notes: String? = null
)

data class SetActivePlanRequest(
    @SerializedName("planId") val planId: Long? = null
)

// ==================== Streak Models ====================

data class StreakSummary(
    @SerializedName("currentStreak") val currentStreak: Int,
    @SerializedName("longestStreak") val longestStreak: Int,
    @SerializedName("totalWorkoutDays") val totalWorkoutDays: Int,
    @SerializedName("totalWorkouts") val totalWorkouts: Int = 0,
    @SerializedName("lastWorkoutDate") val lastWorkoutDate: String? = null
)

// ==================== Statistics Models ====================

data class OverviewStatistics(
    @SerializedName("periodDays") val periodDays: Int,
    @SerializedName("totalSessions") val totalSessions: Int,
    @SerializedName("totalWorkouts") val totalWorkouts: Int = 0,
    @SerializedName("totalMinutes") val totalMinutes: Int = 0,
    @SerializedName("totalCalories") val totalCalories: Int = 0,
    @SerializedName("totalExercises") val totalExercises: Int = 0,
    @SerializedName("totalSets") val totalSets: Int = 0,
    @SerializedName("totalDurationMinutes") val totalDurationMinutes: Int = 0,
    @SerializedName("totalReps") val totalReps: Long = 0,
    @SerializedName("totalVolumeKg") val totalVolumeKg: Double = 0.0,
    @SerializedName("averageSessionDuration") val averageSessionDuration: Int = 0,
    @SerializedName("avgDurationMinutes") val avgDurationMinutes: Int = 0,
    @SerializedName("sessionsPerWeek") val sessionsPerWeek: Double = 0.0
)

data class DailyWorkoutSummary(
    val date: String,
    @SerializedName("sessionCount") val sessionCount: Int = 0,
    @SerializedName("workoutCount") val workoutCount: Int = 0,
    @SerializedName("totalDuration") val totalDuration: Int = 0
)

data class ExerciseStatistics(
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("maxWeightKg") val maxWeightKg: Double?,
    @SerializedName("totalSessions") val totalSessions: Int = 0,
    @SerializedName("totalSets") val totalSets: Int = 0,
    @SerializedName("weightHistory") val weightHistory: List<WeightHistoryEntry> = emptyList()
)

data class WeightHistoryEntry(
    val date: String,
    @SerializedName("maxWeightKg") val maxWeightKg: Double,
    @SerializedName("avgWeightKg") val avgWeightKg: Double? = null,
    @SerializedName("totalSets") val totalSets: Int = 0,
    @SerializedName("totalReps") val totalReps: Int = 0
)

data class ExerciseFrequency(
    @SerializedName("exerciseName") val exerciseName: String,
    val count: Int = 0,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("totalSets") val totalSets: Int = 0
)

data class WeeklyProgress(
    @SerializedName("weekStartDate") val weekStartDate: String,
    @SerializedName("sessionCount") val sessionCount: Int = 0,
    @SerializedName("totalWorkouts") val totalWorkouts: Int = 0,
    @SerializedName("totalMinutes") val totalMinutes: Int = 0,
    @SerializedName("totalDurationMinutes") val totalDurationMinutes: Int = 0,
    @SerializedName("totalVolumeKg") val totalVolumeKg: Double = 0.0
)

data class CalendarDay(
    val date: String,
    @SerializedName("hasWorkout") val hasWorkout: Boolean = false,
    @SerializedName("sessionCount") val sessionCount: Int = 0,
    @SerializedName("workoutCount") val workoutCount: Int = 0,
    @SerializedName("totalDurationMinutes") val totalDurationMinutes: Int = 0
)

// ==================== Reminder Models ====================

data class WorkoutReminder(
    val id: Long,
    val title: String,
    val message: String? = null,
    @SerializedName("reminderTime") val reminderTime: String,
    @SerializedName("daysOfWeek") val daysOfWeek: Set<DayOfWeek> = emptySet(),
    @SerializedName("isEnabled") val isEnabled: Boolean = true,
    @SerializedName("isActive") val isActive: Boolean = true
)

data class CreateReminderRequest(
    val title: String,
    val message: String? = null,
    @SerializedName("reminderTime") val reminderTime: String,
    @SerializedName("daysOfWeek") val daysOfWeek: Set<DayOfWeek>,
    @SerializedName("isEnabled") val isEnabled: Boolean = true
)

data class UpdateReminderRequest(
    val title: String? = null,
    val message: String? = null,
    @SerializedName("reminderTime") val reminderTime: String? = null,
    @SerializedName("daysOfWeek") val daysOfWeek: Set<DayOfWeek>? = null,
    @SerializedName("isEnabled") val isEnabled: Boolean? = null
)

// ==================== Exercise Detail Models ====================

@Immutable
data class WorkoutExerciseDetail(
    val id: Long,
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("lessonId") val lessonId: Long? = null,
    @SerializedName("exerciseOrder") val exerciseOrder: Int = 0,
    val notes: String? = null,
    val sets: List<WorkoutSetDetail> = emptyList(),
    @SerializedName("isFromPlan") val isFromPlan: Boolean = false // Phân biệt bài tập từ kế hoạch vs tự thêm
)

data class WorkoutSetDetail(
    val id: Long,
    @SerializedName("setNumber") val setNumber: Int = 0,
    val reps: Int = 0,
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("isWarmup") val isWarmup: Boolean = false,
    @SerializedName("isCompleted") val isCompleted: Boolean = true,
    val notes: String? = null
)

// ==================== Plan Day Detail Model ====================

data class WorkoutPlanDayDetail(
    val id: Long,
    @SerializedName("dayOfWeek") val dayOfWeek: DayOfWeek,
    val name: String,
    @SerializedName("isRestDay") val isRestDay: Boolean = false,
    val exercises: List<WorkoutPlanExercise> = emptyList()
)

// ==================== Exercise Template Models ====================

data class ExerciseTemplate(
    val id: Long,
    val name: String,
    @SerializedName("categoryId") val categoryId: Long,
    @SerializedName("categoryName") val categoryName: String,
    val description: String
)

data class ExercisePresets(
    @SerializedName("repRanges") val repRanges: List<RepRangePreset>,
    @SerializedName("commonSets") val commonSets: List<Int>,
    @SerializedName("commonReps") val commonReps: List<Int>,
    @SerializedName("restTimes") val restTimes: List<RestTimePreset>
)

data class RepRangePreset(
    val range: String,
    val name: String,
    val description: String
)

data class RestTimePreset(
    val seconds: Int,
    val name: String,
    val description: String
)


data class SetRowData(
    val reps: String = "",
    val weight: String = ""
)

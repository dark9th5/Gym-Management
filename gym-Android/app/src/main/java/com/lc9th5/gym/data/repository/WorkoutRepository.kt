package com.lc9th5.gym.data.repository

import com.google.gson.Gson
import com.lc9th5.gym.data.model.*
import com.lc9th5.gym.data.remote.PagedResponse
import com.lc9th5.gym.data.remote.WorkoutApiService
import retrofit2.Response

class WorkoutRepository(private val apiService: WorkoutApiService) {

    private val gson = Gson()

    // ==================== Session Methods ====================

    suspend fun createSession(request: CreateSessionRequest): Result<WorkoutSession> = safeApiCall {
        apiService.createSession(request)
    }

    suspend fun endSession(sessionId: Long): Result<WorkoutSession> = safeApiCall {
        apiService.endSession(sessionId)
    }

    suspend fun getSessions(page: Int = 0, size: Int = 20): Result<PagedResponse<WorkoutSession>> = safeApiCall {
        apiService.getSessions(page, size)
    }

    suspend fun getTodaySessions(): Result<List<WorkoutSession>> = safeApiCall {
        apiService.getTodaySessions()
    }

    suspend fun getSessionsInRange(startDate: String, endDate: String): Result<List<WorkoutSession>> = safeApiCall {
        apiService.getSessionsInRange(startDate, endDate)
    }

    suspend fun getSessionDetail(sessionId: Long): Result<WorkoutSessionDetail> = safeApiCall {
        apiService.getSessionDetail(sessionId)
    }

    suspend fun deleteSession(sessionId: Long): Result<Unit> = safeApiCallUnit {
        apiService.deleteSession(sessionId)
    }

    suspend fun addExerciseToSession(sessionId: Long, request: AddExerciseRequest): Result<WorkoutExercise> = safeApiCall {
        apiService.addExerciseToSession(sessionId, request)
    }

    suspend fun addSetToExercise(exerciseId: Long, request: AddSetRequest): Result<WorkoutSet> = safeApiCall {
        apiService.addSetToExercise(exerciseId, request)
    }

    suspend fun updateSet(setId: Long, request: UpdateSetRequest): Result<WorkoutSet> = safeApiCall {
        apiService.updateSet(setId, request)
    }

    suspend fun deleteSet(setId: Long): Result<Unit> = safeApiCallUnit {
        apiService.deleteSet(setId)
    }

    suspend fun deleteSessionExercise(exerciseId: Long): Result<Unit> = safeApiCallUnit {
        apiService.deleteSessionExercise(exerciseId)
    }

    // ==================== Plan Methods ====================

    suspend fun createPlan(request: CreatePlanRequest): Result<WorkoutPlan> = safeApiCall {
        apiService.createPlan(request)
    }

    suspend fun getPlans(): Result<List<WorkoutPlan>> = safeApiCall {
        apiService.getPlans()
    }

    suspend fun getActivePlan(): Result<WorkoutPlanDetail?> = safeApiCall {
        apiService.getActivePlan()
    }

    suspend fun getTodayPlan(): Result<WorkoutPlanDay?> = safeApiCall {
        apiService.getTodayPlan()
    }

    suspend fun getPlanDetail(planId: Long): Result<WorkoutPlanDetail> = safeApiCall {
        apiService.getPlanDetail(planId)
    }

    suspend fun getPlanDays(planId: Long): Result<List<WorkoutPlanDay>> = safeApiCall {
        apiService.getPlanDays(planId)
    }

    suspend fun setActivePlan(planId: Long?): Result<WorkoutPlan?> = safeApiCall {
        apiService.setActivePlan(SetActivePlanRequest(planId))
    }

    suspend fun addDayToPlan(planId: Long, request: AddPlanDayRequest): Result<WorkoutPlanDay> = safeApiCall {
        apiService.addDayToPlan(planId, request)
    }

    suspend fun addExerciseToPlanDay(dayId: Long, request: AddPlanExerciseRequest): Result<WorkoutPlanExercise> = safeApiCall {
        apiService.addExerciseToPlanDay(dayId, request)
    }

    suspend fun deletePlan(planId: Long): Result<Unit> = safeApiCallUnit {
        apiService.deletePlan(planId)
    }

    suspend fun deletePlanDay(dayId: Long): Result<Unit> = safeApiCallUnit {
        apiService.deletePlanDay(dayId)
    }

    suspend fun updatePlanExercise(exerciseId: Long, request: UpdatePlanExerciseRequest): Result<WorkoutPlanExercise> = safeApiCall {
        apiService.updatePlanExercise(exerciseId, request)
    }

    suspend fun deletePlanExercise(exerciseId: Long): Result<Unit> = safeApiCallUnit {
        apiService.deletePlanExercise(exerciseId)
    }

    // ==================== Streak Methods ====================

    suspend fun getStreak(): Result<StreakSummary> = safeApiCall {
        apiService.getStreak()
    }

    suspend fun checkStreak(): Result<StreakSummary> = safeApiCall {
        apiService.checkStreak()
    }

    // ==================== Statistics Methods ====================

    suspend fun getOverviewStatistics(days: Int = 30): Result<OverviewStatistics> = safeApiCall {
        apiService.getOverviewStatistics(days)
    }

    suspend fun getWorkoutHistory(days: Int = 30): Result<List<DailyWorkoutSummary>> = safeApiCall {
        apiService.getWorkoutHistory(days)
    }

    suspend fun getExerciseStatistics(
        exerciseName: String, 
        startDate: String? = null, 
        endDate: String? = null
    ): Result<ExerciseStatistics> = safeApiCall {
        apiService.getExerciseStatistics(exerciseName, startDate, endDate)
    }

    suspend fun getFrequentExercises(limit: Int = 10): Result<List<ExerciseFrequency>> = safeApiCall {
        apiService.getFrequentExercises(limit)
    }

    suspend fun getWeeklyProgress(weeks: Int = 4): Result<List<WeeklyProgress>> = safeApiCall {
        apiService.getWeeklyProgress(weeks)
    }

    suspend fun getMonthlyCalendar(year: Int, month: Int): Result<List<CalendarDay>> = safeApiCall {
        apiService.getMonthlyCalendar(year, month)
    }

    // ==================== Reminder Methods ====================

    suspend fun createReminder(request: CreateReminderRequest): Result<WorkoutReminder> = safeApiCall {
        apiService.createReminder(request)
    }

    suspend fun getReminders(): Result<List<WorkoutReminder>> = safeApiCall {
        apiService.getReminders()
    }

    suspend fun getEnabledReminders(): Result<List<WorkoutReminder>> = safeApiCall {
        apiService.getEnabledReminders()
    }

    suspend fun updateReminder(reminderId: Long, request: UpdateReminderRequest): Result<WorkoutReminder> = safeApiCall {
        apiService.updateReminder(reminderId, request)
    }

    suspend fun toggleReminder(reminderId: Long): Result<WorkoutReminder> = safeApiCall {
        apiService.toggleReminder(reminderId)
    }

    suspend fun deleteReminder(reminderId: Long): Result<Unit> = safeApiCallUnit {
        apiService.deleteReminder(reminderId)
    }

    // ==================== Exercise Template Methods ====================

    suspend fun getExerciseTemplates(): Result<List<ExerciseTemplate>> = safeApiCall {
        apiService.getExerciseTemplates()
    }

    suspend fun getExerciseTemplatesByCategory(categoryId: Long): Result<List<ExerciseTemplate>> = safeApiCall {
        apiService.getExerciseTemplatesByCategory(categoryId)
    }

    suspend fun getExercisePresets(): Result<ExercisePresets> = safeApiCall {
        apiService.getExercisePresets()
    }

    // ==================== Helper Methods ====================

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse?.error ?: errorResponse?.message ?: "Unknown error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun safeApiCallUnit(apiCall: suspend () -> Response<Unit>): Result<Unit> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse?.error ?: errorResponse?.message ?: "Unknown error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ErrorResponse(
    val error: String? = null,
    val message: String? = null
)

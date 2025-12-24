package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface WorkoutApiService {
    
    // ==================== Session Endpoints ====================
    
    @POST("workout/sessions")
    suspend fun createSession(
        @Body request: CreateSessionRequest
    ): Response<WorkoutSession>
    
    @POST("workout/sessions/{sessionId}/end")
    suspend fun endSession(
        @Path("sessionId") sessionId: Long
    ): Response<WorkoutSession>
    
    @GET("workout/sessions")
    suspend fun getSessions(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<WorkoutSession>>
    
    @GET("workout/sessions/today")
    suspend fun getTodaySessions(): Response<List<WorkoutSession>>
    
    @GET("workout/sessions/range")
    suspend fun getSessionsInRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<WorkoutSession>>
    
    @GET("workout/sessions/{sessionId}")
    suspend fun getSessionDetail(
        @Path("sessionId") sessionId: Long
    ): Response<WorkoutSessionDetail>
    
    @DELETE("workout/sessions/{sessionId}")
    suspend fun deleteSession(
        @Path("sessionId") sessionId: Long
    ): Response<Unit>
    
    @POST("workout/sessions/{sessionId}/exercises")
    suspend fun addExerciseToSession(
        @Path("sessionId") sessionId: Long,
        @Body request: AddExerciseRequest
    ): Response<WorkoutExercise>
    
    @POST("workout/sessions/exercises/{exerciseId}/sets")
    suspend fun addSetToExercise(
        @Path("exerciseId") exerciseId: Long,
        @Body request: AddSetRequest
    ): Response<WorkoutSet>
    
    @PUT("workout/sessions/sets/{setId}")
    suspend fun updateSet(
        @Path("setId") setId: Long,
        @Body request: UpdateSetRequest
    ): Response<WorkoutSet>

    @DELETE("workout/sessions/sets/{setId}")
    suspend fun deleteSet(
        @Path("setId") setId: Long
    ): Response<Unit>
    
    @DELETE("workout/sessions/exercises/{exerciseId}")
    suspend fun deleteSessionExercise(
        @Path("exerciseId") exerciseId: Long
    ): Response<Unit>
    
    // ==================== Plan Endpoints ====================
    
    @POST("workout/plans")
    suspend fun createPlan(
        @Body request: CreatePlanRequest
    ): Response<WorkoutPlan>
    
    @GET("workout/plans")
    suspend fun getPlans(): Response<List<WorkoutPlan>>
    
    @GET("workout/plans/active")
    suspend fun getActivePlan(): Response<WorkoutPlanDetail?>
    
    @GET("workout/plans/today")
    suspend fun getTodayPlan(): Response<WorkoutPlanDay?>
    
    @GET("workout/plans/{planId}")
    suspend fun getPlanDetail(
        @Path("planId") planId: Long
    ): Response<WorkoutPlanDetail>
    
    @GET("workout/plans/{planId}/days")
    suspend fun getPlanDays(
        @Path("planId") planId: Long
    ): Response<List<WorkoutPlanDay>>
    
    @POST("workout/plans/activate")
    suspend fun setActivePlan(
        @Body request: SetActivePlanRequest
    ): Response<WorkoutPlan>
    
    @POST("workout/plans/{planId}/days")
    suspend fun addDayToPlan(
        @Path("planId") planId: Long,
        @Body request: AddPlanDayRequest
    ): Response<WorkoutPlanDay>
    
    @POST("workout/plans/days/{dayId}/exercises")
    suspend fun addExerciseToPlanDay(
        @Path("dayId") dayId: Long,
        @Body request: AddPlanExerciseRequest
    ): Response<WorkoutPlanExercise>
    
    @DELETE("workout/plans/{planId}")
    suspend fun deletePlan(
        @Path("planId") planId: Long
    ): Response<Unit>
    
    @DELETE("workout/plans/days/{dayId}")
    suspend fun deletePlanDay(
        @Path("dayId") dayId: Long
    ): Response<Unit>
    
    @PUT("workout/plans/exercises/{exerciseId}")
    suspend fun updatePlanExercise(
        @Path("exerciseId") exerciseId: Long,
        @Body request: UpdatePlanExerciseRequest
    ): Response<WorkoutPlanExercise>
    
    @DELETE("workout/plans/exercises/{exerciseId}")
    suspend fun deletePlanExercise(
        @Path("exerciseId") exerciseId: Long
    ): Response<Unit>
    
    // ==================== Streak Endpoints ====================
    
    @GET("workout/streak")
    suspend fun getStreak(): Response<StreakSummary>
    
    @POST("workout/streak/check")
    suspend fun checkStreak(): Response<StreakSummary>
    
    // ==================== Statistics Endpoints ====================
    
    @GET("workout/statistics/overview")
    suspend fun getOverviewStatistics(
        @Query("days") days: Int = 30
    ): Response<OverviewStatistics>
    
    @GET("workout/statistics/history")
    suspend fun getWorkoutHistory(
        @Query("days") days: Int = 30
    ): Response<List<DailyWorkoutSummary>>
    
    @GET("workout/statistics/exercise/{exerciseName}")
    suspend fun getExerciseStatistics(
        @Path("exerciseName") exerciseName: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ExerciseStatistics>
    
    @GET("workout/statistics/frequent-exercises")
    suspend fun getFrequentExercises(
        @Query("limit") limit: Int = 10
    ): Response<List<ExerciseFrequency>>
    
    @GET("workout/statistics/weekly")
    suspend fun getWeeklyProgress(
        @Query("weeks") weeks: Int = 4
    ): Response<List<WeeklyProgress>>
    
    @GET("workout/statistics/calendar/{year}/{month}")
    suspend fun getMonthlyCalendar(
        @Path("year") year: Int,
        @Path("month") month: Int
    ): Response<List<CalendarDay>>
    
    // ==================== Reminder Endpoints ====================
    
    @POST("workout/reminders")
    suspend fun createReminder(
        @Body request: CreateReminderRequest
    ): Response<WorkoutReminder>
    
    @GET("workout/reminders")
    suspend fun getReminders(): Response<List<WorkoutReminder>>
    
    @GET("workout/reminders/enabled")
    suspend fun getEnabledReminders(): Response<List<WorkoutReminder>>
    
    @PUT("workout/reminders/{reminderId}")
    suspend fun updateReminder(
        @Path("reminderId") reminderId: Long,
        @Body request: UpdateReminderRequest
    ): Response<WorkoutReminder>
    
    @POST("workout/reminders/{reminderId}/toggle")
    suspend fun toggleReminder(
        @Path("reminderId") reminderId: Long
    ): Response<WorkoutReminder>
    
    @DELETE("workout/reminders/{reminderId}")
    suspend fun deleteReminder(
        @Path("reminderId") reminderId: Long
    ): Response<Unit>
    
    // ==================== Exercise Template Endpoints ====================
    
    @GET("workout/exercises/templates")
    suspend fun getExerciseTemplates(): Response<List<ExerciseTemplate>>
    
    @GET("workout/exercises/templates/category/{categoryId}")
    suspend fun getExerciseTemplatesByCategory(
        @Path("categoryId") categoryId: Long
    ): Response<List<ExerciseTemplate>>
    
    @GET("workout/exercises/presets")
    suspend fun getExercisePresets(): Response<ExercisePresets>
}

// Paged response wrapper
data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val first: Boolean,
    val last: Boolean
)

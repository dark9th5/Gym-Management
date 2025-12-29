package lc._th5.gym_BE.controller.workout

import lc._th5.gym_BE.service.workout.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workout/statistics")
class StatisticsController(
    private val statisticsService: StatisticsService
) {
    
    @GetMapping("/overview")
    fun getOverview(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "30") days: Int
    ): ResponseEntity<OverviewStatistics> {
        val userId = jwt.getClaim<Long>("uid")
        val stats = statisticsService.getOverviewStatistics(userId, days)
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/history")
    fun getHistory(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "30") days: Int
    ): ResponseEntity<List<DailyWorkoutSummary>> {
        val userId = jwt.getClaim<Long>("uid")
        val history = statisticsService.getWorkoutHistory(userId, days)
        return ResponseEntity.ok(history)
    }
    
    @GetMapping("/exercise/{exerciseName}")
    fun getExerciseStats(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable exerciseName: String
    ): ResponseEntity<ExerciseStatistics> {
        val userId = jwt.getClaim<Long>("uid")
        val stats = statisticsService.getExerciseStatistics(userId, exerciseName)
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/frequent-exercises")
    fun getFrequentExercises(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ExerciseFrequency>> {
        val userId = jwt.getClaim<Long>("uid")
        val exercises = statisticsService.getMostFrequentExercises(userId, limit)
        return ResponseEntity.ok(exercises)
    }
    
    @GetMapping("/weekly")
    fun getWeeklyProgress(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "4") weeks: Int
    ): ResponseEntity<List<WeeklyProgress>> {
        val userId = jwt.getClaim<Long>("uid")
        val progress = statisticsService.getWeeklyProgress(userId, weeks)
        return ResponseEntity.ok(progress)
    }
    
    @GetMapping("/calendar/{year}/{month}")
    fun getMonthlyCalendar(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable year: Int,
        @PathVariable month: Int
    ): ResponseEntity<List<CalendarDay>> {
        val userId = jwt.getClaim<Long>("uid")
        val calendar = statisticsService.getMonthlyCalendar(userId, year, month)
        return ResponseEntity.ok(calendar)
    }
}

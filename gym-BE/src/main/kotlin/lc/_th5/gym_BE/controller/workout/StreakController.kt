package lc._th5.gym_BE.controller.workout

import lc._th5.gym_BE.service.workout.StreakService
import lc._th5.gym_BE.service.workout.StreakSummary
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workout/streak")
class StreakController(
    private val streakService: StreakService
) {
    
    @GetMapping
    fun getStreak(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<StreakSummary> {
        val userId = jwt.getClaim<Long>("uid")
        val streak = streakService.getStreakSummary(userId)
        return ResponseEntity.ok(streak)
    }
    
    @PostMapping("/check")
    fun checkStreak(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<StreakSummary> {
        val userId = jwt.getClaim<Long>("uid")
        // Check and reset if needed, then return summary
        val streak = streakService.getStreakSummary(userId)
        return ResponseEntity.ok(streak)
    }
}

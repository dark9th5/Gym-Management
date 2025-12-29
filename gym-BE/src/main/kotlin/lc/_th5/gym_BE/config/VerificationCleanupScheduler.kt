package lc._th5.gym_BE.config

import lc._th5.gym_BE.service.UserService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class VerificationCleanupScheduler(
    private val userService: UserService
) {
    // Run every 5 minutes to purge expired unverified codes
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    fun purgeExpired() {
        userService.cleanupExpiredCodes()
    }
}

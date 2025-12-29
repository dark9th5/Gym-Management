package lc._th5.gym_BE.repository

import lc._th5.gym_BE.model.user.PendingEmailVerification
import org.springframework.data.jpa.repository.JpaRepository

interface PendingEmailVerificationRepository : JpaRepository<PendingEmailVerification, Long> {
    fun findByEmail(email: String): PendingEmailVerification?
    fun findAllByVerifiedFalseAndExpiresAtBefore(time: java.time.LocalDateTime): List<PendingEmailVerification>
}
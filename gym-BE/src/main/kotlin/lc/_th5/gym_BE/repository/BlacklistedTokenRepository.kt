package lc._th5.gym_BE.repository

import lc._th5.gym_BE.model.user.BlacklistedToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlacklistedTokenRepository : JpaRepository<BlacklistedToken, Long> {
    fun existsByJti(jti: String): Boolean
    fun deleteByExpiresAtBefore(expiresAt: java.time.LocalDateTime)
}
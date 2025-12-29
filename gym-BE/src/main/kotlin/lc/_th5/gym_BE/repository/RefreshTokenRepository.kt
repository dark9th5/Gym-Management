package lc._th5.gym_BE.repository

import lc._th5.gym_BE.model.user.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun deleteByUserId(userId: Long)
}
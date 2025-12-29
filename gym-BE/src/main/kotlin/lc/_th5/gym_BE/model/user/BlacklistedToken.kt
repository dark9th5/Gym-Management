package lc._th5.gym_BE.model.user

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Index
import java.time.LocalDateTime

@Entity
@Table(name = "blacklisted_tokens", indexes = [Index(name = "idx_token_jti", columnList = "jti")])
data class BlacklistedToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val jti: String, // JWT ID
    val expiresAt: LocalDateTime
)
package lc._th5.gym_BE.model.user

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "pending_email_verifications")
data class PendingEmailVerification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var code: String,

    @Column(nullable = false)
    var verified: Boolean = false,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "created_at")
    @CreationTimestamp
    val createdAt: LocalDateTime? = null,

    @Column(name = "last_sent_at")
    var lastSentAt: LocalDateTime? = null
    ,
    @Column(name = "last_attempt_at")
    var lastAttemptAt: LocalDateTime? = null,

    @Column(name = "last_attempt_success")
    var lastAttemptSuccess: Boolean = false
)
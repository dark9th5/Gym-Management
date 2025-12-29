package lc._th5.gym_BE.model.user

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val token: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "created_at")
    @CreationTimestamp
    val createdAt: LocalDateTime? = null
)
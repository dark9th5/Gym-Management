package lc._th5.gym_BE.model.notification

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "system_notifications")
data class SystemNotification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, length = 2000)
    val content: String,

    @Column(name = "created_at")
    @CreationTimestamp
    val createdAt: LocalDateTime? = null,
    
    // Optional: Target specific users or "ALL"
    @Column(name = "target_audience")
    val targetAudience: String = "ALL"
)

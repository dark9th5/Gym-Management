package lc._th5.gym_BE.model.workout

import jakarta.persistence.*
import lc._th5.gym_BE.model.user.User
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * Kế hoạch tập luyện của người dùng
 */
@Entity
@Table(name = "workout_plans")
data class WorkoutPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val name: String, // Tên kế hoạch: "Push Pull Legs", "Full Body 3 ngày"

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "is_active")
    var isActive: Boolean = true, // Kế hoạch đang được sử dụng

    @Column(name = "weeks_duration")
    val weeksDuration: Int? = null, // Số tuần của kế hoạch (null = vô hạn)

    @OneToMany(mappedBy = "plan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val days: MutableList<WorkoutPlanDay> = mutableListOf(),

    @Column(name = "created_at")
    @CreationTimestamp
    val createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    @UpdateTimestamp
    val updatedAt: LocalDateTime? = null
)

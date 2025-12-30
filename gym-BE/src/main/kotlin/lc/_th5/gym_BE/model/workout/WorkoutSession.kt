package lc._th5.gym_BE.model.workout

import jakarta.persistence.*
import lc._th5.gym_BE.model.user.User
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

/**
 * Đại diện cho một buổi tập luyện của người dùng
 * Tất cả các trường thông tin được mã hóa để bảo mật
 */
@Entity
@Table(name = "workout_sessions")
data class WorkoutSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, columnDefinition = "TEXT")
    val name: String, // Tên buổi tập (mã hóa)

    @Column(columnDefinition = "TEXT")
    val notes: String? = null, // Ghi chú (mã hóa)

    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,

    @Column(name = "duration_minutes", columnDefinition = "TEXT")
    var durationMinutes: String? = null, // Thời gian tập (mã hóa)

    @Column(name = "calories_burned", columnDefinition = "TEXT")
    var caloriesBurned: String? = null, // Ước tính calo đốt (mã hóa)

    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true)
    val exercises: MutableList<WorkoutExercise> = mutableListOf(),

    @Column(name = "created_at")
    @CreationTimestamp
    val createdAt: LocalDateTime? = null
)


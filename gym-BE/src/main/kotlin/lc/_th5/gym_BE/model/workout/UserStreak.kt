package lc._th5.gym_BE.model.workout

import jakarta.persistence.*
import lc._th5.gym_BE.model.user.User
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Theo dõi chuỗi ngày tập (streak) của người dùng
 */
@Entity
@Table(name = "user_streaks")
data class UserStreak(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "current_streak", nullable = false)
    var currentStreak: Int = 0, // Chuỗi ngày tập hiện tại

    @Column(name = "longest_streak", nullable = false)
    var longestStreak: Int = 0, // Chuỗi dài nhất từng đạt được

    @Column(name = "last_workout_date")
    var lastWorkoutDate: LocalDate? = null, // Ngày tập gần nhất

    @Column(name = "total_workout_days", nullable = false)
    var totalWorkoutDays: Int = 0, // Tổng số ngày đã tập

    @Column(name = "updated_at")
    @UpdateTimestamp
    val updatedAt: LocalDateTime? = null
)

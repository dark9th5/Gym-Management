package lc._th5.gym_BE.model.workout

import jakarta.persistence.*
import lc._th5.gym_BE.model.user.User
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Nhắc nhở tập luyện của người dùng
 */
@Entity
@Table(name = "workout_reminders")
data class WorkoutReminder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val title: String, // Tiêu đề nhắc nhở

    @Column(columnDefinition = "TEXT")
    val message: String? = null, // Nội dung nhắc nhở

    @Column(name = "reminder_time", nullable = false)
    val reminderTime: LocalTime, // Giờ nhắc nhở

    @ElementCollection
    @CollectionTable(name = "reminder_days", joinColumns = [JoinColumn(name = "reminder_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    val daysOfWeek: MutableSet<DayOfWeek> = mutableSetOf(), // Các ngày trong tuần

    @Column(name = "is_enabled")
    var isEnabled: Boolean = true, // Có bật nhắc nhở không

    @Column(name = "created_at")
    @CreationTimestamp
    val createdAt: LocalDateTime? = null
)

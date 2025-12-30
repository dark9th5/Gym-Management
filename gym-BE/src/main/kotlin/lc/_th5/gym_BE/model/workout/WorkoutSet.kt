package lc._th5.gym_BE.model.workout

import jakarta.persistence.*

/**
 * Đại diện cho một set trong bài tập
 * Tất cả các trường thông tin được mã hóa để bảo mật
 */
@Entity
@Table(name = "workout_sets")
data class WorkoutSet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    val exercise: WorkoutExercise,

    @Column(name = "set_number", nullable = false, columnDefinition = "TEXT")
    val setNumber: String, // Số thứ tự set (mã hóa)

    @Column(nullable = false, columnDefinition = "TEXT")
    val reps: String, // Số rep đã thực hiện (mã hóa)

    @Column(name = "weight_kg", columnDefinition = "TEXT")
    val weightKg: String? = null, // Trọng lượng kg (mã hóa)

    @Column(name = "duration_seconds", columnDefinition = "TEXT")
    val durationSeconds: String? = null, // Thời gian (mã hóa)

    @Column(name = "is_warmup")
    val isWarmup: Boolean = false, // Có phải set khởi động không

    @Column(name = "is_completed")
    var isCompleted: Boolean = true, // Đã hoàn thành chưa

    @Column(columnDefinition = "TEXT")
    val notes: String? = null // Ghi chú (mã hóa)
)


package lc._th5.gym_BE.model.workout

import jakarta.persistence.*

/**
 * Đại diện cho một set trong bài tập
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

    @Column(name = "set_number", nullable = false)
    val setNumber: Int, // Số thứ tự set: 1, 2, 3...

    @Column(nullable = false)
    val reps: Int, // Số rep đã thực hiện

    @Column(name = "weight_kg")
    val weightKg: Double? = null, // Trọng lượng (kg)

    @Column(name = "duration_seconds")
    val durationSeconds: Int? = null, // Thời gian (cho bài tập plank, cardio...)

    @Column(name = "is_warmup")
    val isWarmup: Boolean = false, // Có phải set khởi động không

    @Column(name = "is_completed")
    var isCompleted: Boolean = true, // Đã hoàn thành chưa

    @Column(columnDefinition = "TEXT")
    val notes: String? = null // Ghi chú: "cảm thấy mệt", "form tốt"...
)

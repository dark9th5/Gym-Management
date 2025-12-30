package lc._th5.gym_BE.model.workout

import jakarta.persistence.*
import lc._th5.gym_BE.model.guidance.GuidanceLesson

/**
 * Đại diện cho một bài tập trong buổi tập
 * Tất cả các trường thông tin được mã hóa để bảo mật
 */
@Entity
@Table(name = "workout_exercises")
data class WorkoutExercise(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: WorkoutSession,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    val lesson: GuidanceLesson? = null, // Liên kết với bài tập hướng dẫn (nếu có)

    @Column(name = "exercise_name", nullable = false, columnDefinition = "TEXT")
    val exerciseName: String, // Tên bài tập (mã hóa)

    @Column(name = "exercise_order", columnDefinition = "TEXT")
    val exerciseOrder: String = "0", // Thứ tự bài tập (mã hóa)

    @Column(columnDefinition = "TEXT")
    val notes: String? = null, // Ghi chú (mã hóa)

    @OneToMany(mappedBy = "exercise", cascade = [CascadeType.ALL], orphanRemoval = true)
    val sets: MutableList<WorkoutSet> = mutableListOf()
)


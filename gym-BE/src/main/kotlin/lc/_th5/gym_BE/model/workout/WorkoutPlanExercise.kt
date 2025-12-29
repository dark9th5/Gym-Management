package lc._th5.gym_BE.model.workout

import jakarta.persistence.*
import lc._th5.gym_BE.model.guidance.GuidanceLesson

/**
 * Bài tập trong kế hoạch ngày
 */
@Entity
@Table(name = "workout_plan_exercises")
data class WorkoutPlanExercise(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id", nullable = false)
    val planDay: WorkoutPlanDay,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    var lesson: GuidanceLesson? = null, // Liên kết với bài tập hướng dẫn

    @Column(name = "exercise_name", nullable = false)
    var exerciseName: String, // Tên bài tập

    @Column(name = "exercise_order")
    var exerciseOrder: Int = 0, // Thứ tự

    @Column(name = "target_sets")
    var targetSets: Int = 3, // Số set mục tiêu

    @Column(name = "target_reps")
    var targetReps: String = "8-12", // Số rep mục tiêu (có thể là range)

    @Column(name = "target_weight_kg")
    var targetWeightKg: Double? = null, // Trọng lượng mục tiêu

    @Column(name = "rest_seconds")
    var restSeconds: Int = 60, // Thời gian nghỉ giữa set (giây)

    @Column(columnDefinition = "TEXT")
    var notes: String? = null
)

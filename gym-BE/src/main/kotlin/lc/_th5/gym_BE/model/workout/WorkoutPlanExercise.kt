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
    var exerciseOrder: String, // Thứ tự (mã hóa)

    @Column(name = "target_sets")
    var targetSets: String, // Số set mục tiêu (mã hóa)

    @Column(name = "target_reps")
    var targetReps: String = "8-12", // Số rep mục tiêu (mã hóa)

    @Column(name = "target_weight_kg")
    var targetWeightKg: String? = null, // Trọng lượng mục tiêu (mã hóa)

    @Column(name = "rest_seconds")
    var restSeconds: String, // Thời gian nghỉ giữa set (mã hóa)

    @Column(columnDefinition = "TEXT")
    var notes: String? = null
)

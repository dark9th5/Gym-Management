package lc._th5.gym_BE.model.workout

import jakarta.persistence.*

/**
 * Một ngày trong kế hoạch tập luyện
 */
@Entity
@Table(name = "workout_plan_days")
data class WorkoutPlanDay(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    val plan: WorkoutPlan,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: DayOfWeek, // Thứ trong tuần

    @Column(nullable = false)
    val name: String, // Tên ngày tập: "Push Day", "Ngày nghỉ"

    @Column(name = "is_rest_day")
    val isRestDay: Boolean = false, // Có phải ngày nghỉ không

    @OneToMany(mappedBy = "planDay", cascade = [CascadeType.ALL], orphanRemoval = true)
    val exercises: MutableList<WorkoutPlanExercise> = mutableListOf()
)

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

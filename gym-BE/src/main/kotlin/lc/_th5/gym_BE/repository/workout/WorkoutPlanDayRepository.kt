package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.DayOfWeek
import lc._th5.gym_BE.model.workout.WorkoutPlanDay
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkoutPlanDayRepository : JpaRepository<WorkoutPlanDay, Long> {
    
    fun findByPlanIdOrderByDayOfWeek(planId: Long): List<WorkoutPlanDay>
    
    fun findByPlanIdAndDayOfWeek(planId: Long, dayOfWeek: DayOfWeek): WorkoutPlanDay?
}

package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.WorkoutPlanExercise
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkoutPlanExerciseRepository : JpaRepository<WorkoutPlanExercise, Long> {
    
    fun findByPlanDayIdOrderByExerciseOrder(planDayId: Long): List<WorkoutPlanExercise>
}

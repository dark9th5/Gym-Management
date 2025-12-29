package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.WorkoutPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkoutPlanRepository : JpaRepository<WorkoutPlan, Long> {
    
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<WorkoutPlan>
    
    fun findByUserIdAndIsActiveTrue(userId: Long): WorkoutPlan?
    
    fun existsByUserIdAndName(userId: Long, name: String): Boolean
}

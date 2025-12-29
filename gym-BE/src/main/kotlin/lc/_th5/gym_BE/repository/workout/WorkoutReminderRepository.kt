package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.DayOfWeek
import lc._th5.gym_BE.model.workout.WorkoutReminder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WorkoutReminderRepository : JpaRepository<WorkoutReminder, Long> {
    
    fun findByUserIdOrderByReminderTime(userId: Long): List<WorkoutReminder>
    
    fun findByUserIdAndIsEnabledTrue(userId: Long): List<WorkoutReminder>
    
    @Query("""
        SELECT wr FROM WorkoutReminder wr 
        JOIN wr.daysOfWeek d 
        WHERE wr.isEnabled = true 
        AND d = :dayOfWeek
    """)
    fun findEnabledRemindersByDayOfWeek(@Param("dayOfWeek") dayOfWeek: DayOfWeek): List<WorkoutReminder>
}

package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.WorkoutExercise
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WorkoutExerciseRepository : JpaRepository<WorkoutExercise, Long> {
    
    fun findBySessionIdOrderByExerciseOrder(sessionId: Long): List<WorkoutExercise>
    
    @Query("""
        SELECT we.exerciseName, COUNT(we) as count 
        FROM WorkoutExercise we 
        WHERE we.session.user.id = :userId 
        GROUP BY we.exerciseName 
        ORDER BY count DESC
    """)
    fun getMostFrequentExercises(@Param("userId") userId: Long): List<Array<Any>>
    
    @Query("""
        SELECT COUNT(we) 
        FROM WorkoutExercise we 
        WHERE we.session.user.id = :userId 
        AND we.session.startedAt >= :startDate
    """)
    fun getTotalExerciseCount(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: java.time.LocalDateTime
    ): Long?
}

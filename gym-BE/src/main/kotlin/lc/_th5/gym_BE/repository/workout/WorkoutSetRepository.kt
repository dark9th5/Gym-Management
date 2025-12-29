package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.WorkoutSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WorkoutSetRepository : JpaRepository<WorkoutSet, Long> {
    
    fun findByExerciseIdOrderBySetNumber(exerciseId: Long): List<WorkoutSet>
    
    @Query("""
        SELECT SUM(ws.reps) 
        FROM WorkoutSet ws 
        WHERE ws.exercise.session.user.id = :userId 
        AND ws.exercise.session.startedAt >= :startDate
    """)
    fun getTotalReps(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: java.time.LocalDateTime
    ): Long?
    
    @Query("""
        SELECT SUM(ws.weightKg * ws.reps) 
        FROM WorkoutSet ws 
        WHERE ws.exercise.session.user.id = :userId 
        AND ws.exercise.session.startedAt >= :startDate
    """)
    fun getTotalVolume(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: java.time.LocalDateTime
    ): Double?
    
    @Query("""
        SELECT MAX(ws.weightKg) 
        FROM WorkoutSet ws 
        WHERE ws.exercise.exerciseName = :exerciseName 
        AND ws.exercise.session.user.id = :userId
    """)
    fun getMaxWeightForExercise(
        @Param("userId") userId: Long,
        @Param("exerciseName") exerciseName: String
    ): Double?
    
    @Query("""
        SELECT COUNT(ws) 
        FROM WorkoutSet ws 
        WHERE ws.exercise.session.user.id = :userId 
        AND ws.exercise.session.startedAt >= :startDate
    """)
    fun getTotalSetCount(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: java.time.LocalDateTime
    ): Long?
    
    @Query("""
        SELECT DATE(we.session.startedAt) as date, 
               MAX(ws.weightKg) as maxWeight, 
               AVG(ws.weightKg) as avgWeight,
               COUNT(ws) as totalSets,
               SUM(ws.reps) as totalReps
        FROM WorkoutSet ws
        JOIN ws.exercise we
        WHERE we.exerciseName = :exerciseName 
        AND we.session.user.id = :userId
        AND ws.weightKg IS NOT NULL
        GROUP BY DATE(we.session.startedAt)
        ORDER BY DATE(we.session.startedAt) DESC
    """)
    fun getWeightHistoryForExercise(
        @Param("userId") userId: Long,
        @Param("exerciseName") exerciseName: String
    ): List<Array<Any>>
}

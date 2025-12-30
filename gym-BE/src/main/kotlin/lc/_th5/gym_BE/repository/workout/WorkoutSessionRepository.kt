package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.WorkoutSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface WorkoutSessionRepository : JpaRepository<WorkoutSession, Long> {
    
    fun findByUserIdOrderByStartedAtDesc(userId: Long): List<WorkoutSession>
    
    fun findByUserIdOrderByStartedAtDesc(userId: Long, pageable: Pageable): Page<WorkoutSession>
    
    fun findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<WorkoutSession>
    
    @Query("SELECT COUNT(DISTINCT DATE(ws.startedAt)) FROM WorkoutSession ws WHERE ws.user.id = :userId")
    fun countDistinctWorkoutDaysByUserId(@Param("userId") userId: Long): Int
    
    @Query("SELECT ws FROM WorkoutSession ws WHERE ws.user.id = :userId AND DATE(ws.startedAt) = CURRENT_DATE")
    fun findTodaySessionsByUserId(@Param("userId") userId: Long): List<WorkoutSession>
    
    // Note: Các query aggregate (SUM, AVG) đã bị xóa vì dữ liệu được mã hóa
    // Việc tính toán thống kê giờ được thực hiện trên client
}

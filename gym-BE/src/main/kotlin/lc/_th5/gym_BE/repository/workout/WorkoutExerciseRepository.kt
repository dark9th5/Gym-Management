package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.WorkoutExercise
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkoutExerciseRepository : JpaRepository<WorkoutExercise, Long> {
    
    // Note: findBySessionIdOrderByExerciseOrder không hoạt động vì exerciseOrder giờ là String mã hóa
    // Sử dụng findBySessionId và sort ở tầng service/controller
    fun findBySessionId(sessionId: Long): List<WorkoutExercise>
    
    // Note: Các query aggregate đã bị xóa vì dữ liệu được mã hóa
}

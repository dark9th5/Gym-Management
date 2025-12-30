package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.WorkoutSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkoutSetRepository : JpaRepository<WorkoutSet, Long> {
    
    // Note: findByExerciseIdOrderBySetNumber không hoạt động vì setNumber giờ là String mã hóa
    // Sử dụng findByExerciseId và sort ở tầng service/controller
    fun findByExerciseId(exerciseId: Long): List<WorkoutSet>
    
    // Note: Các query aggregate (SUM, MAX, AVG) đã bị xóa vì dữ liệu được mã hóa
    // Việc tính toán thống kê giờ được thực hiện trên client
}

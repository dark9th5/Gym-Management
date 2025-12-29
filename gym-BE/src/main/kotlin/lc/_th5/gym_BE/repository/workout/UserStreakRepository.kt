package lc._th5.gym_BE.repository.workout

import lc._th5.gym_BE.model.workout.UserStreak
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserStreakRepository : JpaRepository<UserStreak, Long> {
    
    fun findByUserId(userId: Long): UserStreak?
}

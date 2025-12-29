package lc._th5.gym_BE.service.workout

import lc._th5.gym_BE.model.workout.UserStreak
import lc._th5.gym_BE.repository.UserRepository
import lc._th5.gym_BE.repository.workout.UserStreakRepository
import lc._th5.gym_BE.repository.workout.WorkoutSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class StreakService(
    private val streakRepository: UserStreakRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val userRepository: UserRepository
) {
    
    fun getUserStreak(userId: Long): UserStreak {
        return streakRepository.findByUserId(userId) ?: createInitialStreak(userId)
    }
    
    @Transactional
    fun updateStreak(userId: Long): UserStreak {
        val streak = getUserStreak(userId)
        val today = LocalDate.now()
        
        // Nếu đã tập hôm nay rồi thì không cập nhật
        if (streak.lastWorkoutDate == today) {
            return streak
        }
        
        val lastDate = streak.lastWorkoutDate
        
        if (lastDate == null) {
            // Lần đầu tập
            streak.currentStreak = 1
            streak.longestStreak = 1
            streak.totalWorkoutDays = 1
        } else {
            val daysSinceLastWorkout = ChronoUnit.DAYS.between(lastDate, today)
            
            when {
                daysSinceLastWorkout == 1L -> {
                    // Tập liên tiếp
                    streak.currentStreak += 1
                    if (streak.currentStreak > streak.longestStreak) {
                        streak.longestStreak = streak.currentStreak
                    }
                }
                daysSinceLastWorkout > 1L -> {
                    // Bị gián đoạn, reset streak
                    streak.currentStreak = 1
                }
                // daysSinceLastWorkout == 0 đã được handle ở trên
            }
            streak.totalWorkoutDays += 1
        }
        
        streak.lastWorkoutDate = today
        
        return streakRepository.save(streak)
    }
    
    @Transactional
    fun checkAndResetStreak(userId: Long): UserStreak {
        val streak = getUserStreak(userId)
        val today = LocalDate.now()
        
        if (streak.lastWorkoutDate != null) {
            val daysSinceLastWorkout = ChronoUnit.DAYS.between(streak.lastWorkoutDate, today)
            if (daysSinceLastWorkout > 1) {
                // Streak bị broken
                streak.currentStreak = 0
                return streakRepository.save(streak)
            }
        }
        
        return streak
    }
    
    fun getStreakSummary(userId: Long): StreakSummary {
        val streak = checkAndResetStreak(userId)
        val totalDays = sessionRepository.countDistinctWorkoutDaysByUserId(userId)
        
        return StreakSummary(
            currentStreak = streak.currentStreak,
            longestStreak = streak.longestStreak,
            totalWorkoutDays = totalDays,
            lastWorkoutDate = streak.lastWorkoutDate
        )
    }
    
    private fun createInitialStreak(userId: Long): UserStreak {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val streak = UserStreak(user = user)
        return streakRepository.save(streak)
    }
}

data class StreakSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalWorkoutDays: Int,
    val lastWorkoutDate: LocalDate?
)

package lc._th5.gym_BE.service

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class LoginAttemptService {
    private val attempts = ConcurrentHashMap<String, MutableList<LocalDateTime>>()
    private val maxAttempts = 5
    private val blockDurationMinutes = 15L

    fun loginFailed(key: String) {
        val now = LocalDateTime.now()
        attempts.computeIfAbsent(key) { mutableListOf() }.add(now)
        // Clean old attempts
        attempts[key]?.removeIf { it.isBefore(now.minusMinutes(blockDurationMinutes)) }
    }

    fun loginSucceeded(key: String) {
        attempts.remove(key)
    }

    fun isBlocked(key: String): Boolean {
        val now = LocalDateTime.now()
        val userAttempts = attempts[key] ?: return false
        // Remove expired attempts
        userAttempts.removeIf { it.isBefore(now.minusMinutes(blockDurationMinutes)) }
        return userAttempts.size >= maxAttempts
    }

    fun getRemainingAttempts(key: String): Int {
        val now = LocalDateTime.now()
        val userAttempts = attempts[key] ?: return maxAttempts
        userAttempts.removeIf { it.isBefore(now.minusMinutes(blockDurationMinutes)) }
        return maxAttempts - userAttempts.size
    }
}
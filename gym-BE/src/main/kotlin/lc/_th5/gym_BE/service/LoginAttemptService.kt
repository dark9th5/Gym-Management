package lc._th5.gym_BE.service

import lc._th5.gym_BE.util.EncryptedMemoryService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class LoginAttemptService(
    private val encryptedMemoryService: EncryptedMemoryService
) {
    private val attempts = ConcurrentHashMap<String, MutableList<LocalDateTime>>()
    private val maxAttempts = 5
    private val blockDurationMinutes = 15L

    fun loginFailed(key: String) {
        val encryptedKey = encryptedMemoryService.encryptKey(key)
        val now = LocalDateTime.now()
        attempts.computeIfAbsent(encryptedKey) { mutableListOf() }.add(now)
        // Clean old attempts
        attempts[encryptedKey]?.removeIf { it.isBefore(now.minusMinutes(blockDurationMinutes)) }
    }

    fun loginSucceeded(key: String) {
        val encryptedKey = encryptedMemoryService.encryptKey(key)
        attempts.remove(encryptedKey)
    }

    fun isBlocked(key: String): Boolean {
        val encryptedKey = encryptedMemoryService.encryptKey(key)
        val now = LocalDateTime.now()
        val userAttempts = attempts[encryptedKey] ?: return false
        // Remove expired attempts
        userAttempts.removeIf { it.isBefore(now.minusMinutes(blockDurationMinutes)) }
        return userAttempts.size >= maxAttempts
    }

    fun getRemainingAttempts(key: String): Int {
        val encryptedKey = encryptedMemoryService.encryptKey(key)
        val now = LocalDateTime.now()
        val userAttempts = attempts[encryptedKey] ?: return maxAttempts
        userAttempts.removeIf { it.isBefore(now.minusMinutes(blockDurationMinutes)) }
        return maxAttempts - userAttempts.size
    }
}
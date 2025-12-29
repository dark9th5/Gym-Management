package lc._th5.gym_BE.service

import lc._th5.gym_BE.model.user.Role
import lc._th5.gym_BE.model.user.User
import lc._th5.gym_BE.model.user.PendingEmailVerification
import lc._th5.gym_BE.repository.UserRepository
import lc._th5.gym_BE.repository.PendingEmailVerificationRepository
import lc._th5.gym_BE.util.EncryptionService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory
import java.util.*
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
    private val pendingEmailVerificationRepository: PendingEmailVerificationRepository,
    private val encryptionService: EncryptionService
) {
    companion object {
        private const val VERIFICATION_CODE_TTL_MINUTES: Long = 15
        private const val RESEND_COOLDOWN_SECONDS: Long = 30
    }
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    @Transactional
    fun sendEmailVerification(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw IllegalArgumentException("Email already registered")
        }
        val code = UUID.randomUUID().toString().substring(0, 6).uppercase()
        val existingPending = pendingEmailVerificationRepository.findByEmail(normalizedEmail)
        val now = LocalDateTime.now()
        // Throttle resends: only if last send was successful (avoid forcing wait when previous attempt failed)
        if (existingPending != null && existingPending.lastSentAt != null && existingPending.lastAttemptSuccess) {
            val lastSentAt = existingPending.lastSentAt!!
            val allowedAt = lastSentAt.plusSeconds(RESEND_COOLDOWN_SECONDS)
            if (now.isBefore(allowedAt)) {
                val remaining = java.time.Duration.between(now, allowedAt).seconds
                throw IllegalStateException("Vui lòng đợi ${remaining}s trước khi gửi lại mã")
            }
        }

        val expiresAt = now.plusMinutes(VERIFICATION_CODE_TTL_MINUTES)
        val pending = if (existingPending != null) {
            existingPending.code = code
            existingPending.verified = false
            existingPending.expiresAt = expiresAt
            // do not set lastSentAt yet; set only after successful send
            existingPending
        } else {
            // lastSentAt left null until successful send so failed attempts don't trigger cooldown
            PendingEmailVerification(email = normalizedEmail, code = code, expiresAt = expiresAt, lastSentAt = null)
        }
        // save pending state (without lastSentAt)
        pendingEmailVerificationRepository.save(pending)
        // record attempt
        pending.lastAttemptAt = now
        pending.lastAttemptSuccess = false
        pendingEmailVerificationRepository.save(pending)
        try {
            emailService.sendVerificationEmail(normalizedEmail, code)
            // mark lastSentAt and success only after successful send
            pending.lastSentAt = now
            pending.lastAttemptSuccess = true
            pendingEmailVerificationRepository.save(pending)
            logger.info("[UserService] Successfully sent verification email to {}", normalizedEmail)
            return true
        } catch (e: Exception) {
            logger.error("[UserService] Failed to send verification email: {}", e.message)
            logger.error("[UserService] Stacktrace:", e)
            // leave lastSentAt as null (or unchanged) so cooldown doesn't block; lastAttemptSuccess=false recorded
            throw IllegalArgumentException("Không thể gửi mã xác thực: ${e.message}")
        }
    }

    fun verifyEmailCode(email: String, code: String): Boolean {
        val normalized = email.trim().lowercase()
        logger.info("[UserService] verifyEmailCode called for email={}, code={}", normalized, code)
        val pending = pendingEmailVerificationRepository.findByEmail(normalized)
            ?: run {
                logger.info("[UserService] No pending verification found for {}", normalized)
                return false
            }
        // Check expiration
        if (pending.expiresAt.isBefore(LocalDateTime.now())) {
            logger.info("[UserService] Verification code expired for {}", normalized)
            return false
        }
        return if (pending.code == code.uppercase()) {
            pending.verified = true
            pendingEmailVerificationRepository.save(pending)
            logger.info("[UserService] Verification succeeded for {}", normalized)
            true
        } else {
            logger.info("[UserService] Verification failed for {} (code mismatch)", normalized)
            false
        }
    }

    @Transactional
    fun cleanupExpiredCodes() {
        val now = LocalDateTime.now()
        val expired = pendingEmailVerificationRepository.findAllByVerifiedFalseAndExpiresAtBefore(now)
        if (expired.isNotEmpty()) {
            pendingEmailVerificationRepository.deleteAll(expired)
        }
    }

    fun isEmailVerified(email: String): Boolean {
        val pending = pendingEmailVerificationRepository.findByEmail(email.trim().lowercase())
        return pending?.verified == true
    }

    @Transactional
    fun register(username: String, email: String, rawPassword: String, fullName: String?): User {
        if (userRepository.existsByEmail(email.trim().lowercase())) {
            throw IllegalArgumentException("Email already registered")
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw IllegalArgumentException("Username already taken")
        }
        if (fullName.isNullOrBlank()) {
            throw IllegalArgumentException("Full name is required")
        }
        // Mã hóa fullName trước khi lưu
        val user = User(
            username = username.trim(),
            email = email.trim().lowercase(),
            password = passwordEncoder.encode(rawPassword),
            fullName = encryptionService.encrypt(fullName.trim()),
            roles = mutableSetOf(Role.USER),
            isVerified = true
        )
        val savedUser = userRepository.save(user)
    
        pendingEmailVerificationRepository.findByEmail(email.trim().lowercase())?.let {
            pendingEmailVerificationRepository.delete(it)
        }
        return savedUser
    }

    fun findByEmail(email: String): User? =
        userRepository.findByEmail(email.trim().lowercase())

    /**
     * Find user by ID
     */
    fun findById(id: Long): User? = userRepository.findById(id).orElse(null)

    /**
     * Update user profile
     */
    @Transactional
    fun updateProfile(userId: Long, fullName: String?, newUsername: String? = null): User {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        
        // Check if new username is taken
        if (!newUsername.isNullOrBlank() && newUsername.trim() != user.username) {
            if (userRepository.existsByUsername(newUsername.trim())) {
                throw IllegalArgumentException("Tên người dùng đã được sử dụng")
            }
        }
        
        // Mã hóa fullName khi cập nhật
        val updated = user.copy(
            fullName = fullName?.trim()?.let { encryptionService.encrypt(it) } ?: user.fullName,
            username = newUsername?.trim() ?: user.username
        )
        return userRepository.save(updated)
    }
    
    /**
     * Change user password
     */
    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw IllegalArgumentException("Mật khẩu hiện tại không đúng")
        }
        
        // Validate new password
        if (newPassword.length < 6) {
            throw IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự")
        }
        
        val updated = user.copy(password = passwordEncoder.encode(newPassword))
        userRepository.save(updated)
    }
}
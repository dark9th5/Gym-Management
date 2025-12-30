package lc._th5.gym_BE.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev.samstevens.totp.code.CodeGenerator
import dev.samstevens.totp.code.CodeVerifier
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.secret.SecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import lc._th5.gym_BE.model.user.User
import lc._th5.gym_BE.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.Base64

/**
 * Service xử lý 2FA với TOTP (Time-based One-Time Password)
 * Tương thích với Google Authenticator, Authy, Microsoft Authenticator, etc.
 */
@Service
class TotpService(
    private val userRepository: UserRepository,
    private val encryptionService: lc._th5.gym_BE.util.EncryptionService
) {
    companion object {
        private const val ISSUER = "GymApp"  // Tên hiển thị trong Authenticator app
        private const val SECRET_LENGTH = 32  // Độ dài secret key
        private const val BACKUP_CODE_COUNT = 8  // Số lượng backup codes
        private const val BACKUP_CODE_LENGTH = 8  // Độ dài mỗi backup code
    }
    
    private val secretGenerator: SecretGenerator = DefaultSecretGenerator(SECRET_LENGTH)
    private val codeGenerator: CodeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA1)
    private val codeVerifier: CodeVerifier = DefaultCodeVerifier(codeGenerator, SystemTimeProvider())
    private val qrGenerator = ZxingPngQrGenerator()
    private val objectMapper = ObjectMapper()
    
    /**
     * Bước 1: Khởi tạo setup 2FA - tạo secret và QR code
     * User chưa enable 2FA, chỉ tạo secret tạm thời
     */
    @Transactional
    fun initiate2faSetup(userId: Long): TwoFactorSetupResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        if (user.is2faEnabled) {
            throw IllegalStateException("2FA đã được bật. Vui lòng tắt trước khi thiết lập lại.")
        }
        
        // Tạo secret key mới
        val secret = secretGenerator.generate()
        
        // Mã hóa secret trước khi lưu
        val encryptedSecret = encryptionService.encrypt(secret)
        
        // Lưu secret tạm thời (chưa enable)
        val updatedUser = user.copy(totpSecret = encryptedSecret, is2faEnabled = false)
        userRepository.save(updatedUser)
        
        // Tạo QR code data (dùng secret gốc - chưa mã hóa)
        val qrData = QrData.Builder()
            .label(user.email)
            .secret(secret)
            .issuer(ISSUER)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build()
        
        // Generate QR code image (Base64)
        val qrCodeImage = try {
            val imageData = qrGenerator.generate(qrData)
            Base64.getEncoder().encodeToString(imageData)
        } catch (e: Exception) {
            null
        }
        
        // Tạo manual entry key (cho trường hợp không scan được QR)
        val manualEntryKey = formatSecretForManualEntry(secret)
        
        return TwoFactorSetupResponse(
            secret = secret, // Trả về secret gốc cho client setup
            qrCodeBase64 = qrCodeImage,
            manualEntryKey = manualEntryKey,
            issuer = ISSUER,
            accountName = user.email
        )
    }
    
    /**
     * Bước 2: Xác nhận và enable 2FA
     * User phải nhập mã từ Authenticator app để xác nhận đã setup đúng
     */
    @Transactional
    fun verify2faSetup(userId: Long, code: String): TwoFactorVerifyResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val encryptedSecret = user.totpSecret
            ?: throw IllegalStateException("Chưa khởi tạo 2FA. Vui lòng gọi /2fa/setup trước.")
            
        // Giải mã secret để verify
        val secret = encryptionService.safeDecrypt(encryptedSecret)
        
        if (user.is2faEnabled) {
            throw IllegalStateException("2FA đã được bật rồi.")
        }
        
        // Verify code
        if (!codeVerifier.isValidCode(secret, code)) {
            return TwoFactorVerifyResponse(
                success = false,
                message = "Mã xác thực không đúng. Vui lòng thử lại.",
                backupCodes = null
            )
        }
        
        // Tạo backup codes
        val backupCodes = generateBackupCodes()
        val backupCodesJson = objectMapper.writeValueAsString(backupCodes)
        val encryptedBackupCodes = encryptionService.encrypt(backupCodesJson)
        
        // Enable 2FA
        val updatedUser = user.copy(
            is2faEnabled = true,
            backupCodes = encryptedBackupCodes
        )
        userRepository.save(updatedUser)
        
        return TwoFactorVerifyResponse(
            success = true,
            message = "2FA đã được bật thành công!",
            backupCodes = backupCodes
        )
    }
    
    /**
     * Xác thực mã TOTP khi đăng nhập
     */
    fun verifyCode(user: User, code: String): Boolean {
        val encryptedSecret = user.totpSecret ?: return false
        val secret = encryptionService.safeDecrypt(encryptedSecret)
        return codeVerifier.isValidCode(secret, code)
    }
    
    /**
     * Xác thực backup code (khi mất điện thoại)
     */
    @Transactional
    fun verifyBackupCode(userId: Long, code: String): Boolean {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val encryptedBackupCodes = user.backupCodes ?: return false
        val backupCodesJson = encryptionService.safeDecrypt(encryptedBackupCodes)
        
        val backupCodes = try {
            objectMapper.readValue(backupCodesJson, Array<String>::class.java).toMutableList()
        } catch (e: Exception) {
            return false
        }
        
        val normalizedCode = code.replace("-", "").replace(" ", "").uppercase()
        
        if (backupCodes.contains(normalizedCode)) {
            // Remove used backup code
            backupCodes.remove(normalizedCode)
            val newBackupCodesJson = objectMapper.writeValueAsString(backupCodes)
            val newEncryptedBackupCodes = encryptionService.encrypt(newBackupCodesJson)
            
            val updatedUser = user.copy(
                backupCodes = newEncryptedBackupCodes
            )
            userRepository.save(updatedUser)
            return true
        }
        
        return false
    }
    
    /**
     * Tắt 2FA
     */
    @Transactional
    fun disable2fa(userId: Long, code: String): Boolean {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        if (!user.is2faEnabled) {
            throw IllegalStateException("2FA chưa được bật.")
        }
        
        // Verify code before disabling
        if (!verifyCode(user, code) && !verifyBackupCode(userId, code)) {
            return false
        }
        
        val updatedUser = user.copy(
            is2faEnabled = false,
            totpSecret = null,
            backupCodes = null
        )
        userRepository.save(updatedUser)
        return true
    }
    
    /**
     * Lấy trạng thái 2FA của user
     */
    fun get2faStatus(userId: Long): TwoFactorStatusResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val backupCodesRemaining = if (user.backupCodes != null) {
            try {
                val backupCodesJson = encryptionService.safeDecrypt(user.backupCodes!!)
                objectMapper.readValue(backupCodesJson, Array<String>::class.java).size
            } catch (e: Exception) {
                0
            }
        } else 0
        
        return TwoFactorStatusResponse(
            is2faEnabled = user.is2faEnabled,
            hasSecret = user.totpSecret != null,
            backupCodesRemaining = backupCodesRemaining
        )
    }
    
    /**
     * Tạo backup codes mới (khi hết hoặc muốn đổi)
     */
    @Transactional
    fun regenerateBackupCodes(userId: Long, code: String): List<String>? {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        if (!user.is2faEnabled) {
            throw IllegalStateException("2FA chưa được bật.")
        }
        
        // Verify current TOTP code
        if (!verifyCode(user, code)) {
            return null
        }
        
        val newBackupCodes = generateBackupCodes()
        val newBackupCodesJson = objectMapper.writeValueAsString(newBackupCodes)
        val encryptedBackupCodes = encryptionService.encrypt(newBackupCodesJson)
        
        val updatedUser = user.copy(
            backupCodes = encryptedBackupCodes
        )
        userRepository.save(updatedUser)
        
        return newBackupCodes
    }
    
    // ==================== Private Helper Methods ====================
    
    private fun generateBackupCodes(): List<String> {
        val random = SecureRandom()
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        
        return (1..BACKUP_CODE_COUNT).map {
            (1..BACKUP_CODE_LENGTH).map {
                chars[random.nextInt(chars.length)]
            }.joinToString("")
        }
    }
    
    private fun formatSecretForManualEntry(secret: String): String {
        // Format thành groups of 4 characters cho dễ đọc
        return secret.chunked(4).joinToString(" ")
    }
}

// ==================== DTOs ====================

data class TwoFactorSetupResponse(
    val secret: String,
    val qrCodeBase64: String?,
    val manualEntryKey: String,
    val issuer: String,
    val accountName: String
)

data class TwoFactorVerifyResponse(
    val success: Boolean,
    val message: String,
    val backupCodes: List<String>?
)

data class TwoFactorStatusResponse(
    val is2faEnabled: Boolean,
    val hasSecret: Boolean,
    val backupCodesRemaining: Int
)

data class TwoFactorVerifyRequest(
    val code: String
)

data class TwoFactorLoginRequest(
    val tempToken: String,
    val code: String
)

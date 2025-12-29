package lc._th5.gym_BE.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service mã hóa tin nhắn sử dụng AES-256-GCM
 * 
 * SECURITY FEATURES:
 * - AES-256: Thuật toán mã hóa mạnh nhất
 * - GCM Mode: Đảm bảo tính toàn vẹn dữ liệu (Authenticated Encryption)
 * - Random IV: Mỗi tin nhắn có IV riêng, chống replay attack
 * 
 * CÁCH SỬ DỤNG:
 * - encrypt(): Mã hóa tin nhắn trước khi lưu vào database
 * - decrypt(): Giải mã tin nhắn khi đọc từ database
 */
@Service
class EncryptionService {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256  // bits
        private const val GCM_IV_LENGTH = 12  // bytes (recommended for GCM)
        private const val GCM_TAG_LENGTH = 128 // bits
        
        /**
         * Tạo secret key mới (chỉ dùng 1 lần khi setup)
         * Trả về chuỗi 32 ký tự ngẫu nhiên (alphanumeric)
         */
        @JvmStatic
        fun generateKey(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            val random = SecureRandom()
            return (1..32).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }
    }

    @Value("\${encryption.secret.key:#{null}}")
    private var secretKeyConfig: String? = null

    private val secretKey: SecretKey by lazy {
        val keyString = secretKeyConfig 
            ?: System.getenv("ENCRYPTION_SECRET_KEY")
            ?: throw IllegalStateException(
                "ENCRYPTION_SECRET_KEY không được cấu hình! " +
                "Vui lòng set environment variable ENCRYPTION_SECRET_KEY (32 ký tự)"
            )
        
        // Chấp nhận chuỗi 32 ký tự trực tiếp (ASCII bytes)
        val keyBytes = if (keyString.length == 32) {
            // Chuỗi 32 ký tự -> dùng trực tiếp làm 32 bytes (256 bits)
            keyString.toByteArray(Charsets.UTF_8)
        } else {
            // Fallback: thử decode Base64 (cho backward compatibility)
            try {
                Base64.getDecoder().decode(keyString)
            } catch (e: Exception) {
                throw IllegalStateException(
                    "ENCRYPTION_SECRET_KEY phải là chuỗi 32 ký tự. Hiện tại: ${keyString.length} ký tự"
                )
            }
        }
        
        if (keyBytes.size != 32) {
            throw IllegalStateException(
                "ENCRYPTION_SECRET_KEY phải có đúng 32 bytes. Hiện tại: ${keyBytes.size} bytes"
            )
        }
        
        SecretKeySpec(keyBytes, "AES")
    }

    private val secureRandom = SecureRandom()

    /**
     * Mã hóa một chuỗi văn bản
     * 
     * @param plainText Văn bản cần mã hóa
     * @return Chuỗi Base64 chứa [IV + Ciphertext + Auth Tag]
     */
    fun encrypt(plainText: String): String {
        // Generate random IV for each encryption
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Combine IV + CipherText (GCM appends auth tag to ciphertext automatically)
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Giải mã một chuỗi đã mã hóa
     * 
     * @param encryptedText Chuỗi Base64 đã mã hóa
     * @return Văn bản gốc
     * @throws SecurityException nếu tin nhắn bị thay đổi (failed auth tag)
     */
    fun decrypt(encryptedText: String): String {
        val combined = Base64.getDecoder().decode(encryptedText)

        // Extract IV and CipherText
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }

    /**
     * Kiểm tra xem một chuỗi có phải là tin nhắn đã mã hóa không
     * (Dùng để backward compatibility với tin nhắn cũ chưa mã hóa)
     */
    fun isEncrypted(text: String): Boolean {
        return try {
            val decoded = Base64.getDecoder().decode(text)
            decoded.size > GCM_IV_LENGTH
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Giải mã an toàn - trả về văn bản gốc nếu không phải encrypted
     */
    fun safeDecrypt(text: String): String {
        return if (isEncrypted(text)) {
            try {
                decrypt(text)
            } catch (e: Exception) {
                // Nếu decrypt thất bại, có thể là Base64 string bình thường
                text
            }
        } else {
            text
        }
    }
}

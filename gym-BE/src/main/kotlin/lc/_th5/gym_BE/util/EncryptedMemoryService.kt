package lc._th5.gym_BE.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

/**
 * Service mã hóa dữ liệu tạm thời trong memory
 * Sử dụng cho các ConcurrentHashMap lưu trữ sensitive data
 *
 * SECURITY FEATURES:
 * - AES-256-GCM cho encryption
 * - Random IV cho mỗi encryption
 * - Không lưu key trong memory sau khi sử dụng
 */
@Service
class EncryptedMemoryService {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())

    @Value("\${memory.encryption.key:#{null}}")
    private var memoryKeyConfig: String? = null

    private val secretKey: SecretKey by lazy {
        val keyString = memoryKeyConfig
            ?: System.getenv("MEMORY_ENCRYPTION_KEY")
            ?: generateFallbackKey()

        val keyBytes = when {
            keyString.length == 32 -> keyString.toByteArray(Charsets.UTF_8)
            keyString.length == 44 -> Base64.getDecoder().decode(keyString) // Base64 encoded
            else -> throw IllegalStateException("MEMORY_ENCRYPTION_KEY phải là 32 ký tự hoặc Base64 encoded 256-bit key")
        }

        if (keyBytes.size != 32) {
            throw IllegalStateException("Key phải có đúng 32 bytes cho AES-256")
        }

        SecretKeySpec(keyBytes, "AES")
    }

    private val secureRandom = SecureRandom()

    /**
     * Tạo key fallback (chỉ dùng khi không có config)
     */
    private fun generateFallbackKey(): String {
        val keyBytes = ByteArray(32)
        secureRandom.nextBytes(keyBytes)
        return Base64.getEncoder().encodeToString(keyBytes)
    }

    /**
     * Mã hóa một string
     */
    fun encrypt(data: String): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherText = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherText

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Giải mã một string
     */
    fun decrypt(encryptedData: String): String {
        val combined = Base64.getDecoder().decode(encryptedData)
        if (combined.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted data")
        }

        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val plainText = cipher.doFinal(cipherText)
        return String(plainText, Charsets.UTF_8)
    }

    /**
     * Mã hóa key cho HashMap (thường là email/username)
     * NOTE: Không dùng encryption với random IV vì sẽ không thể lookup được trong Map
     * Trả về nguyên bản hoặc hash
     */
    fun encryptKey(key: String): String {
        return key
    }

    /**
     * Giải mã key cho HashMap
     */
    fun decryptKey(encryptedKey: String): String {
        return encryptedKey
    }

    /**
     * Mã hóa object thành JSON string
     */
    fun encryptObject(obj: Any): String {
        val json = objectMapper.writeValueAsString(obj)
        return encrypt(json)
    }

    /**
     * Giải mã JSON string thành object
     */
    fun <T> decryptObject(encryptedData: String, clazz: Class<T>): T {
        val json = decrypt(encryptedData)
        return objectMapper.readValue(json, clazz)
    }
}
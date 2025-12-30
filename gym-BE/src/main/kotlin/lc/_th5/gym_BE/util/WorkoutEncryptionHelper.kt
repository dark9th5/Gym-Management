package lc._th5.gym_BE.util

import org.springframework.stereotype.Component

/**
 * Helper để mã hóa/giải mã dữ liệu workout của người dùng
 * 
 * Các trường được mã hóa:
 * - workout_plans: name, description
 * - workout_plan_days: name
 * - workout_plan_exercises: exerciseName, exerciseOrder, targetSets, targetReps, targetWeightKg, restSeconds, notes
 * - workout_sessions: name, notes, durationMinutes, caloriesBurned
 * - workout_exercises: exerciseName, exerciseOrder, notes
 * - workout_sets: setNumber, reps, weightKg, durationSeconds, notes
 */
@Component
class WorkoutEncryptionHelper(
    private val encryptionService: EncryptionService
) {
    
    // ==================== ENCRYPT STRING ====================
    
    /**
     * Mã hóa tên (không null)
     */
    fun encryptName(name: String): String {
        return encryptionService.encrypt(name)
    }
    
    /**
     * Mã hóa text có thể null
     */
    fun encryptNullable(text: String?): String? {
        return text?.let { encryptionService.encrypt(it) }
    }
    
    // ==================== ENCRYPT NUMBERS ====================
    
    /**
     * Mã hóa số nguyên
     */
    fun encryptInt(value: Int): String {
        return encryptionService.encrypt(value.toString())
    }
    
    /**
     * Mã hóa số nguyên có thể null
     */
    fun encryptIntNullable(value: Int?): String? {
        return value?.let { encryptionService.encrypt(it.toString()) }
    }
    
    /**
     * Mã hóa số thực
     */
    fun encryptDouble(value: Double): String {
        return encryptionService.encrypt(value.toString())
    }
    
    /**
     * Mã hóa số thực có thể null
     */
    fun encryptDoubleNullable(value: Double?): String? {
        return value?.let { encryptionService.encrypt(it.toString()) }
    }
    
    // ==================== DECRYPT STRING ====================
    
    /**
     * Giải mã tên (không null)
     */
    fun decryptName(encryptedName: String): String {
        return encryptionService.safeDecrypt(encryptedName)
    }
    
    /**
     * Giải mã text có thể null
     */
    fun decryptNullable(encryptedText: String?): String? {
        return encryptedText?.let { encryptionService.safeDecrypt(it) }
    }
    
    // ==================== DECRYPT NUMBERS ====================
    
    /**
     * Giải mã số nguyên
     */
    fun decryptInt(encryptedValue: String): Int {
        return try {
            encryptionService.safeDecrypt(encryptedValue).toInt()
        } catch (e: Exception) {
            // Nếu không giải mã được hoặc không parse được, trả về giá trị gốc nếu là số
            encryptedValue.toIntOrNull() ?: 0
        }
    }
    
    /**
     * Giải mã số nguyên có thể null
     */
    fun decryptIntNullable(encryptedValue: String?): Int? {
        return encryptedValue?.let {
            try {
                encryptionService.safeDecrypt(it).toIntOrNull()
            } catch (e: Exception) {
                it.toIntOrNull()
            }
        }
    }
    
    /**
     * Giải mã số thực
     */
    fun decryptDouble(encryptedValue: String): Double {
        return try {
            encryptionService.safeDecrypt(encryptedValue).toDouble()
        } catch (e: Exception) {
            encryptedValue.toDoubleOrNull() ?: 0.0
        }
    }
    
    /**
     * Giải mã số thực có thể null
     */
    fun decryptDoubleNullable(encryptedValue: String?): Double? {
        return encryptedValue?.let {
            try {
                encryptionService.safeDecrypt(it).toDoubleOrNull()
            } catch (e: Exception) {
                it.toDoubleOrNull()
            }
        }
    }
}

package lc._th5.gym_BE.util

import org.springframework.stereotype.Component

/**
 * Helper để mã hóa/giải mã dữ liệu workout của người dùng
 * 
 * Các trường được mã hóa:
 * - workout_plans: name, description
 * - workout_plan_exercises: exerciseName, notes
 * - workout_sessions: name, notes  
 * - workout_exercises: exerciseName, notes
 * - workout_sets: notes
 */
@Component
class WorkoutEncryptionHelper(
    private val encryptionService: EncryptionService
) {
    
    // ==================== ENCRYPT (Lưu vào DB) ====================
    
    /**
     * Mã hóa tên (không null)
     */
    fun encryptName(name: String): String {
        return encryptionService.encrypt(name)
    }
    
    /**
     * Mã hóa description/notes (có thể null)
     */
    fun encryptNullable(text: String?): String? {
        return text?.let { encryptionService.encrypt(it) }
    }
    
    // ==================== DECRYPT (Đọc từ DB) ====================
    
    /**
     * Giải mã tên (không null)
     */
    fun decryptName(encryptedName: String): String {
        return encryptionService.safeDecrypt(encryptedName)
    }
    
    /**
     * Giải mã description/notes (có thể null)
     */
    fun decryptNullable(encryptedText: String?): String? {
        return encryptedText?.let { encryptionService.safeDecrypt(it) }
    }
    
    // ==================== BATCH OPERATIONS ====================
    
    /**
     * Data class để hold encrypted workout plan data
     */
    data class EncryptedPlanData(
        val name: String,
        val description: String?
    )
    
    /**
     * Mã hóa dữ liệu plan
     */
    fun encryptPlanData(name: String, description: String?): EncryptedPlanData {
        return EncryptedPlanData(
            name = encryptName(name),
            description = encryptNullable(description)
        )
    }
    
    /**
     * Data class để hold encrypted exercise data
     */
    data class EncryptedExerciseData(
        val exerciseName: String,
        val notes: String?
    )
    
    /**
     * Mã hóa dữ liệu exercise
     */
    fun encryptExerciseData(exerciseName: String, notes: String?): EncryptedExerciseData {
        return EncryptedExerciseData(
            exerciseName = encryptName(exerciseName),
            notes = encryptNullable(notes)
        )
    }
}

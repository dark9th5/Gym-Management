package lc._th5.gym_BE.controller.workout

import lc._th5.gym_BE.repository.guidance.GuidanceLessonRepository
import lc._th5.gym_BE.repository.guidance.GuidanceCategoryRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workout/exercises")
class ExerciseTemplateController(
    private val lessonRepository: GuidanceLessonRepository,
    private val categoryRepository: GuidanceCategoryRepository
) {
    
    /**
     * Lấy danh sách tất cả bài tập từ Guidance Lessons
     * Dùng để người dùng chọn bài tập thay vì tự nhập
     */
    @GetMapping("/templates")
    fun getExerciseTemplates(): ResponseEntity<List<ExerciseTemplateResponse>> {
        val lessons = lessonRepository.findAll()
        val templates = lessons.map { lesson ->
            ExerciseTemplateResponse(
                id = lesson.id,
                name = lesson.title,
                categoryId = lesson.category.id,
                categoryName = lesson.category.displayName,
                description = lesson.description
            )
        }
        return ResponseEntity.ok(templates)
    }
    
    /**
     * Lấy danh sách bài tập theo category
     */
    @GetMapping("/templates/category/{categoryId}")
    fun getExerciseTemplatesByCategory(
        @PathVariable categoryId: Long
    ): ResponseEntity<List<ExerciseTemplateResponse>> {
        val lessons = lessonRepository.findByCategoryIdOrderByLessonNumber(categoryId)
        val templates = lessons.map { lesson ->
            ExerciseTemplateResponse(
                id = lesson.id,
                name = lesson.title,
                categoryId = lesson.category.id,
                categoryName = lesson.category.displayName,
                description = lesson.description
            )
        }
        return ResponseEntity.ok(templates)
    }
    
    /**
     * Lấy các sets/reps phổ biến để người dùng chọn nhanh
     */
    @GetMapping("/presets")
    fun getExercisePresets(): ResponseEntity<ExercisePresetsResponse> {
        val presets = ExercisePresetsResponse(
            repRanges = listOf(
                RepRangePreset("1-5", "Sức mạnh", "Dành cho tập sức mạnh tối đa"),
                RepRangePreset("6-8", "Sức mạnh + Cơ bắp", "Cân bằng giữa sức mạnh và tăng cơ"),
                RepRangePreset("8-12", "Tăng cơ", "Tối ưu cho phát triển cơ bắp"),
                RepRangePreset("12-15", "Tăng cơ + Sức bền", "Cân bằng giữa cơ bắp và sức bền"),
                RepRangePreset("15-20", "Sức bền", "Dành cho sức bền cơ bắp"),
                RepRangePreset("20+", "Cardio/Sức bền cao", "Dành cho sức bền và giảm mỡ")
            ),
            commonSets = listOf(3, 4, 5),
            commonReps = listOf(5, 6, 8, 10, 12, 15, 20),
            restTimes = listOf(
                RestTimePreset(30, "Ngắn", "Cho bài tập cách ly, sức bền"),
                RestTimePreset(60, "Trung bình", "Cho hầu hết bài tập"),
                RestTimePreset(90, "Dài", "Cho bài tập compound nặng"),
                RestTimePreset(120, "Rất dài", "Cho bài tập sức mạnh tối đa"),
                RestTimePreset(180, "Tối đa", "Cho nâng tạ nặng nhất")
            )
        )
        return ResponseEntity.ok(presets)
    }
}

// Response DTOs
data class ExerciseTemplateResponse(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val categoryName: String,
    val description: String
)

data class ExercisePresetsResponse(
    val repRanges: List<RepRangePreset>,
    val commonSets: List<Int>,
    val commonReps: List<Int>,
    val restTimes: List<RestTimePreset>
)

data class RepRangePreset(
    val range: String,
    val name: String,
    val description: String
)

data class RestTimePreset(
    val seconds: Int,
    val name: String,
    val description: String
)

package lc._th5.gym_BE.model.guidance

data class CreateLessonRequest(
    val categoryId: Long,
    val title: String,
    val content: String,
    val videoUrl: String? = null,
    val imageUrl: String? = null
)

data class UpdateLessonRequest(
    val title: String,
    val content: String,
    val videoUrl: String? = null,
    val imageUrl: String? = null
)
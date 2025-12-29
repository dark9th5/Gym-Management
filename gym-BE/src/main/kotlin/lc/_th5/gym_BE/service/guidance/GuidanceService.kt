package lc._th5.gym_BE.service.guidance

import lc._th5.gym_BE.model.guidance.*
import lc._th5.gym_BE.repository.guidance.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GuidanceService(
    private val categoryRepository: GuidanceCategoryRepository,
    private val lessonRepository: GuidanceLessonRepository,
    private val detailRepository: GuidanceLessonDetailRepository
) {

    fun getAllCategories(): List<GuidanceCategory> {
        return categoryRepository.findAll()
    }

    fun getLessonsByCategory(categoryId: Long): List<GuidanceLesson> {
        return lessonRepository.findByCategoryIdOrderByLessonNumber(categoryId)
    }

    fun getLessonDetail(lessonId: Long): GuidanceLessonDetail? {
        return detailRepository.findByLessonId(lessonId)
    }

    fun createLesson(request: CreateLessonRequest): GuidanceLesson {
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { IllegalArgumentException("Category not found") }

        val maxLessonNumber = lessonRepository.findByCategoryIdOrderByLessonNumber(request.categoryId)
            .maxOfOrNull { it.lessonNumber } ?: 0

        val lesson = GuidanceLesson(
            title = request.title,
            lessonNumber = maxLessonNumber + 1,
            category = category
        )

        val savedLesson = lessonRepository.save(lesson)

        val detail = GuidanceLessonDetail(
            content = request.content,
            videoUrl = request.videoUrl,
            imageUrl = request.imageUrl,
            lesson = savedLesson
        )

        detailRepository.save(detail)

        return savedLesson
    }

    fun updateLesson(lessonId: Long, request: UpdateLessonRequest): GuidanceLesson {
        val lesson = lessonRepository.findById(lessonId)
            .orElseThrow { IllegalArgumentException("Lesson not found") }

        val updatedLesson = lesson.copy(
            title = request.title
        )

        val savedLesson = lessonRepository.save(updatedLesson)

        val detail = detailRepository.findByLessonId(lessonId)
        if (detail != null) {
            val updatedDetail = detail.copy(
                content = request.content,
                videoUrl = request.videoUrl,
                imageUrl = request.imageUrl
            )
            detailRepository.save(updatedDetail)
        }

        return savedLesson
    }

    @Transactional
    fun deleteLesson(lessonId: Long) {
        val lesson = lessonRepository.findById(lessonId)
            .orElseThrow { IllegalArgumentException("Lesson not found") }

        detailRepository.deleteByLessonId(lessonId)
        lessonRepository.delete(lesson)
    }
}
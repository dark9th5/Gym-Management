package lc._th5.gym_BE.repository.guidance

import lc._th5.gym_BE.model.guidance.GuidanceLesson
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GuidanceLessonRepository : JpaRepository<GuidanceLesson, Long> {
    fun findByCategoryIdOrderByLessonNumber(categoryId: Long): List<GuidanceLesson>
}
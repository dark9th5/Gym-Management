package lc._th5.gym_BE.repository.guidance

import lc._th5.gym_BE.model.guidance.GuidanceLessonDetail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GuidanceLessonDetailRepository : JpaRepository<GuidanceLessonDetail, Long> {
    fun findByLessonId(lessonId: Long): GuidanceLessonDetail?
    fun deleteByLessonId(lessonId: Long)
}
package lc._th5.gym_BE.repository.guidance

import lc._th5.gym_BE.model.guidance.GuidanceCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GuidanceCategoryRepository : JpaRepository<GuidanceCategory, Long> {
}
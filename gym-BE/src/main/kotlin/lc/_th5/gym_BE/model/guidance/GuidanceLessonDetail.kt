package lc._th5.gym_BE.model.guidance

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "guidance_lesson_details")
data class GuidanceLessonDetail(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(columnDefinition = "TEXT")
    val content: String,

    @Column
    val videoUrl: String? = null,

    @Column
    val imageUrl: String? = null,

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    val lesson: GuidanceLesson
)
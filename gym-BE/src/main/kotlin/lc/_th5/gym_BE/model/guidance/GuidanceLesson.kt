package lc._th5.gym_BE.model.guidance

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*

@Entity
@Table(
    name = "guidance_lessons",
    uniqueConstraints = [UniqueConstraint(columnNames = ["category_id", "lesson_number"])]
)
data class GuidanceLesson(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val title: String,

    @Column(name = "lesson_number", nullable = false)
    val lessonNumber: Int,

    @Column(nullable = false)
    val description: String = "",

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: GuidanceCategory,

    @JsonManagedReference
    @OneToOne(mappedBy = "lesson", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val detail: GuidanceLessonDetail? = null
)
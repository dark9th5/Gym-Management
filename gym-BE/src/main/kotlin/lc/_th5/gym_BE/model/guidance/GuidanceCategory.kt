package lc._th5.gym_BE.model.guidance

import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "guidance_categories")
data class GuidanceCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @JsonIgnore
    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val lessons: List<GuidanceLesson> = emptyList()
)
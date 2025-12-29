package lc._th5.gym_BE.controller.guidance

import lc._th5.gym_BE.model.guidance.*
import lc._th5.gym_BE.service.guidance.GuidanceService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/guides")
class GuidanceController(
    private val guidanceService: GuidanceService
) {

    @GetMapping("/categories")
    fun getCategories(): ResponseEntity<List<GuidanceCategory>> {
        val categories = guidanceService.getAllCategories()
        return ResponseEntity.ok(categories)
    }

    @GetMapping("/lessons/{categoryId}")
    fun getLessons(@PathVariable categoryId: Long): ResponseEntity<List<GuidanceLesson>> {
        val lessons = guidanceService.getLessonsByCategory(categoryId)
        return ResponseEntity.ok(lessons)
    }

    @GetMapping("/lessons/detail/{lessonId}")
    fun getLessonDetail(@PathVariable lessonId: Long): ResponseEntity<GuidanceLessonDetail> {
        val detail = guidanceService.getLessonDetail(lessonId)
        return if (detail != null) {
            ResponseEntity.ok(detail)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/lessons")
    @PreAuthorize("hasRole('ADMIN')")
    fun createLesson(@RequestBody request: CreateLessonRequest): ResponseEntity<GuidanceLesson> {
        val lesson = guidanceService.createLesson(request)
        return ResponseEntity.ok(lesson)
    }

    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateLesson(@PathVariable lessonId: Long, @RequestBody request: UpdateLessonRequest): ResponseEntity<GuidanceLesson> {
        val lesson = guidanceService.updateLesson(lessonId, request)
        return ResponseEntity.ok(lesson)
    }

    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteLesson(@PathVariable lessonId: Long): ResponseEntity<Void> {
        guidanceService.deleteLesson(lessonId)
        return ResponseEntity.noContent().build()
    }
}
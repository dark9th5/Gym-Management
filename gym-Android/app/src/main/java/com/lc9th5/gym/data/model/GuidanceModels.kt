package com.lc9th5.gym.data.model

import com.google.gson.annotations.SerializedName

data class GuidanceCategory(
    val id: Long,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("name") val slug: String
)

data class GuidanceLesson(
    val id: Long,
    val title: String,
    @SerializedName("lessonNumber") val sequenceNumber: Int = 0,
    val thumbnail: String? = null
)

data class GuidanceLessonDetail(
    val id: Long,
    val content: String,
    val videoUrl: String?,
    val imageUrl: String?
)

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

package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.CreateLessonRequest
import com.lc9th5.gym.data.model.GuidanceCategory
import com.lc9th5.gym.data.model.GuidanceLesson
import com.lc9th5.gym.data.model.GuidanceLessonDetail
import com.lc9th5.gym.data.model.UpdateLessonRequest
import retrofit2.Response
import retrofit2.http.*

interface GuidanceApiService {

    @GET("guides/categories")
    suspend fun getCategories(): Response<List<GuidanceCategory>>

    @GET("guides/lessons/{categoryId}")
    suspend fun getLessonsByCategory(
        @Path("categoryId") categoryId: Long
    ): Response<List<GuidanceLesson>>

    @GET("guides/lessons/detail/{lessonId}")
    suspend fun getLessonDetail(
        @Path("lessonId") lessonId: Long
    ): Response<GuidanceLessonDetail>

    @POST("guides/lessons")
    suspend fun createLesson(
        @Body request: CreateLessonRequest
    ): Response<GuidanceLesson>

    @PUT("guides/lessons/{lessonId}")
    suspend fun updateLesson(
        @Path("lessonId") lessonId: Long,
        @Body request: UpdateLessonRequest
    ): Response<GuidanceLesson>

    @DELETE("guides/lessons/{lessonId}")
    suspend fun deleteLesson(
        @Path("lessonId") lessonId: Long
    ): Response<Void>
}

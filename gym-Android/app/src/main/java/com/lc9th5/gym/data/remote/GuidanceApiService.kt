package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.GuidanceCategory
import com.lc9th5.gym.data.model.GuidanceLesson
import com.lc9th5.gym.data.model.GuidanceLessonDetail
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GuidanceApiService {

    @GET("/api/guides/categories")
    suspend fun getCategories(): Response<List<GuidanceCategory>>

    @GET("/api/guides/lessons/{categoryId}")
    suspend fun getLessonsByCategory(
        @Path("categoryId") categoryId: Long
    ): Response<List<GuidanceLesson>>

    @GET("/api/guides/lessons/detail/{lessonId}")
    suspend fun getLessonDetail(
        @Path("lessonId") lessonId: Long
    ): Response<GuidanceLessonDetail>
}

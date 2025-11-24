package com.lc9th5.gym.data.repository

import com.google.gson.Gson
import com.lc9th5.gym.data.model.GuidanceCategory
import com.lc9th5.gym.data.model.GuidanceLesson
import com.lc9th5.gym.data.model.GuidanceLessonDetail
import com.lc9th5.gym.data.remote.GuidanceApiService
import retrofit2.Response

class GuidanceRepository(private val apiService: GuidanceApiService) {

    private val gson = Gson()

    suspend fun getCategories(): Result<List<GuidanceCategory>> = safeApiCall {
        apiService.getCategories()
    }

    suspend fun getLessons(categoryId: Long): Result<List<GuidanceLesson>> = safeApiCall {
        apiService.getLessonsByCategory(categoryId)
    }

    suspend fun getLessonDetail(lessonId: Long): Result<GuidanceLessonDetail> = safeApiCall {
        apiService.getLessonDetail(lessonId)
    }

    private inline fun <reified T> safeApiCall(apiCall: () -> Response<T>): Result<T> {
        return try {
            val response = apiCall()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.message ?: "Không xác định"}"))
        }
    }

    private fun <T> parseError(response: Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val errorMap = gson.fromJson(errorBody, Map::class.java)
                errorMap["message"] as? String
                    ?: errorMap["error"] as? String
                    ?: "Không thể tải dữ liệu hướng dẫn"
            } else {
                "Không thể tải dữ liệu hướng dẫn"
            }
        } catch (_: Exception) {
            "Không thể tải dữ liệu hướng dẫn"
        }
    }
}

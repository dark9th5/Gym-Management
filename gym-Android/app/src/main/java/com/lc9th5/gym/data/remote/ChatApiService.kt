package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.ChatHistoryItem
import com.lc9th5.gym.data.model.ChatRequest
import com.lc9th5.gym.data.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface ChatApiService {

    /**
     * Gửi tin nhắn và nhận phản hồi từ AI chatbot
     */
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): Response<ChatResponse>

    /**
     * Lấy lịch sử chat của user hiện tại
     */
    @GET("chat/history")
    suspend fun getHistory(): Response<List<ChatHistoryItem>>

    /**
     * Xóa toàn bộ lịch sử chat
     */
    @DELETE("chat/history")
    suspend fun clearHistory(): Response<Map<String, String>>

    /**
     * Kiểm tra trạng thái chatbot service
     */
    @GET("chat/health")
    suspend fun healthCheck(): Response<Map<String, String>>
}

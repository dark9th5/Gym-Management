package com.lc9th5.gym.data.model

import java.time.LocalDateTime

/**
 * Tin nhắn trong chat (dùng trong UI)
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Request gửi đến API chat
 */
data class ChatRequest(
    val message: String,
    val conversationId: String? = null
)

/**
 * Response từ API chat
 */
data class ChatResponse(
    val reply: String,
    val conversationId: String,
    val timestamp: String
)

/**
 * Item lịch sử chat từ server
 */
data class ChatHistoryItem(
    val id: Long,
    val content: String,
    val isFromUser: Boolean,
    val createdAt: String // ISO DateTime string
)

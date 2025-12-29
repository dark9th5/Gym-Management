package lc._th5.gym_BE.model.chat

import java.time.LocalDateTime

/**
 * Request body cho API chat
 */
data class ChatRequest(
    val message: String,
    val conversationId: String? = null
)

/**
 * Response body từ API chat
 */
data class ChatResponse(
    val reply: String,
    val conversationId: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Cấu hình cho Gemini API request
 */
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig = GenerationConfig(),
    val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user"
)

data class GeminiPart(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val maxOutputTokens: Int = 2048
)

/**
 * Response từ Gemini API
 */
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)



package lc._th5.gym_BE.service

import lc._th5.gym_BE.model.chat.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@Service
class ChatbotService {

    @Value("\${gemini.api.key:}")
    private lateinit var geminiApiKey: String

    @Value("\${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private lateinit var geminiApiUrl: String

    private val webClient = WebClient.builder()
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
        }
        .build()

    // Rate limiting: track last request time
    private val lastRequestTime = AtomicLong(0)
    private val minRequestInterval = 4000L // 4 seconds between requests (15 RPM limit)

    // Fallback responses khi bị rate limit
    private val fallbackResponses = listOf(
        "💪 Tập luyện đều đặn và nghỉ ngơi đầy đủ là chìa khóa thành công! Hãy thử lại sau vài giây nhé.",
        "🏋️ Mẹo: Luôn khởi động 5-10 phút trước khi tập. Tôi đang bận, thử lại sau nhé!",
        "🥗 Protein rất quan trọng cho việc xây dựng cơ bắp. Hãy hỏi lại sau vài giây!",
        "⏰ Nghỉ ngơi 48h giữa các buổi tập cùng nhóm cơ. Tôi sẽ trả lời chi tiết hơn sau!",
        "💧 Uống đủ nước (2-3 lít/ngày) khi tập gym. Thử hỏi lại sau nhé!"
    )

    // Từ khóa liên quan đến gym/fitness để lọc câu hỏi
    private val gymKeywords = listOf(
        // Tiếng Việt
        "gym", "tập", "luyện", "thể hình", "cơ bắp", "cơ", "bụng", "ngực", "vai", "lưng", "chân", "tay",
        "cardio", "chạy", "đạp xe", "bơi", "aerobic", "hiit",
        "tạ", "dumbbell", "barbell", "máy tập", "xà đơn", "xà kép", "day", "đẩy", "kéo", "squat", "deadlift",
        "protein", "whey", "bcaa", "creatine", "supplement", "thực phẩm", "dinh dưỡng", "ăn", "uống", "calo", "calories",
        "giảm cân", "tăng cân", "giảm mỡ", "tăng cơ", "bulk", "cut", "lean", "béo", "gầy", "cân nặng",
        "khởi động", "giãn cơ", "nghỉ ngơi", "phục hồi", "chấn thương", "đau", "mỏi",
        "kế hoạch", "lịch tập", "bài tập", "set", "rep", "hiệp",
        "sức khỏe", "thể lực", "thể thao", "vận động", "fitness", "workout", "exercise", "training",
        // Tiếng Anh phổ biến
        "muscle", "weight", "lift", "bench", "press", "curl", "row", "pull", "push",
        "diet", "nutrition", "body", "fat", "slim", "strong", "strength", "endurance",
        // Chào hỏi cơ bản (cho phép)
        "xin chào", "chào", "hello", "hi", "hey", "help", "giúp", "hỏi", "tư vấn"
    )

    // Thông báo từ chối câu hỏi không liên quan
    private val offTopicResponse = """
        🏋️ Xin lỗi, tôi là **Gym AI Assistant** - chuyên gia về tập gym và dinh dưỡng thể thao.
        
        Tôi chỉ có thể hỗ trợ các câu hỏi liên quan đến:
        • 💪 Bài tập thể hình, cardio
        • 🥗 Dinh dưỡng, chế độ ăn
        • 📋 Kế hoạch tập luyện
        • 🏃 Giảm cân, tăng cơ
        
        Hãy hỏi tôi về gym nhé! 😊
    """.trimIndent()

    /**
     * Kiểm tra xem câu hỏi có liên quan đến gym không
     */
    private fun isGymRelated(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return gymKeywords.any { keyword -> lowerMessage.contains(keyword.lowercase()) }
    }


    // System prompt để chatbot focus vào chủ đề gym/fitness
    private val systemPrompt = """
        Bạn là một huấn luyện viên thể hình chuyên nghiệp và chuyên gia dinh dưỡng thể thao. 
        Tên của bạn là "Gym AI Assistant".
        
        Nhiệm vụ của bạn:
        - Tư vấn về các bài tập thể hình, cardio, và sức mạnh
        - Hướng dẫn kỹ thuật tập luyện đúng cách
        - Tư vấn dinh dưỡng để tăng cơ, giảm mỡ
        - Lập kế hoạch tập luyện phù hợp
        - Giải đáp thắc mắc về sức khỏe liên quan đến tập gym
        
        Nguyên tắc:
        - Trả lời bằng tiếng Việt, thân thiện và dễ hiểu
        - Luôn khuyến khích người dùng tập luyện an toàn
        - Nếu câu hỏi không liên quan đến gym/fitness, hãy nhẹ nhàng hướng người dùng quay lại chủ đề
        - Đưa ra lời khuyên cụ thể và có thể thực hiện được
        - Sử dụng emoji để làm tin nhắn sinh động hơn
        - Giữ câu trả lời ngắn gọn, dưới 200 từ
    """.trimIndent()

    /**
     * Xử lý tin nhắn từ người dùng và trả về phản hồi từ AI
     */
    fun processMessage(userMessage: String, conversationId: String?): ChatResponse {
        val actualConversationId = conversationId ?: UUID.randomUUID().toString()

        // Nếu không có API key, trả về response mặc định
        if (geminiApiKey.isBlank()) {
            return ChatResponse(
                reply = "Xin chào! 👋 Tôi là Gym AI Assistant. Hiện tại tôi đang trong chế độ demo. " +
                        "Hãy hỏi tôi bất cứ điều gì về tập gym, dinh dưỡng, hay kế hoạch tập luyện nhé! 💪",
                conversationId = actualConversationId
            )
        }

        // Kiểm tra xem câu hỏi có liên quan đến gym không
        // Nếu không liên quan -> trả lời local, không gọi API (tiết kiệm tài nguyên)
        if (!isGymRelated(userMessage)) {
            return ChatResponse(
                reply = offTopicResponse,
                conversationId = actualConversationId
            )
        }

        // Rate limiting: đợi nếu request quá nhanh
        val now = System.currentTimeMillis()
        val lastTime = lastRequestTime.get()
        val waitTime = minRequestInterval - (now - lastTime)
        
        if (waitTime > 0) {
            Thread.sleep(waitTime)
        }
        lastRequestTime.set(System.currentTimeMillis())

        return try {
            callGeminiWithRetry(userMessage, actualConversationId, maxRetries = 2)
        } catch (e: Exception) {
            val errorMessage = e.message ?: ""
            
            // Xử lý rate limit error
            if (errorMessage.contains("429") || errorMessage.contains("Too Many Requests")) {
                ChatResponse(
                    reply = fallbackResponses.random() + "\n\n⚠️ _Gemini API đang bận, vui lòng đợi 10-15 giây rồi thử lại._",
                    conversationId = actualConversationId
                )
            } else {
                ChatResponse(
                    reply = "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau! 🙏\n\n_Lỗi: ${e.message?.take(100)}_",
                    conversationId = actualConversationId
                )
            }
        }
    }

    /**
     * Gọi Gemini API với retry logic
     */
    private fun callGeminiWithRetry(userMessage: String, conversationId: String, maxRetries: Int): ChatResponse {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val geminiRequest = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(userMessage)),
                            role = "user"
                        )
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(systemPrompt)),
                        role = "user"
                    )
                )

                val response = webClient.post()
                    .uri("$geminiApiUrl?key=$geminiApiKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(geminiRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError) { clientResponse ->
                        if (clientResponse.statusCode().value() == 429) {
                            throw WebClientResponseException(
                                429, "Too Many Requests", null, null, null
                            )
                        }
                        clientResponse.createException()
                    }
                    .bodyToMono(GeminiResponse::class.java)
                    .timeout(Duration.ofSeconds(30))
                    .block()

                val reply = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Xin lỗi, tôi không thể xử lý câu hỏi này. Bạn có thể hỏi lại được không? 🙏"

                return ChatResponse(
                    reply = reply,
                    conversationId = conversationId
                )
            } catch (e: Exception) {
                lastException = e
                val errorMsg = e.message ?: ""
                
                // Nếu là rate limit, đợi rồi retry
                if (errorMsg.contains("429") || errorMsg.contains("Too Many Requests")) {
                    val backoffTime = (attempt + 1) * 5000L // 5s, 10s
                    Thread.sleep(backoffTime)
                } else {
                    throw e // Các lỗi khác throw ngay
                }
            }
        }
        
        throw lastException ?: RuntimeException("Unknown error after retries")
    }
}

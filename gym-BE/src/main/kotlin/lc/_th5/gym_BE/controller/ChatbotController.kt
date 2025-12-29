package lc._th5.gym_BE.controller

import lc._th5.gym_BE.model.chat.ChatMessageEntity
import lc._th5.gym_BE.model.chat.ChatRequest
import lc._th5.gym_BE.model.chat.ChatResponse
import lc._th5.gym_BE.repository.ChatMessageRepository
import lc._th5.gym_BE.repository.UserRepository
import lc._th5.gym_BE.service.ChatbotService
import lc._th5.gym_BE.util.EncryptionService
import lc._th5.gym_BE.util.XssSanitizer
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * DTO cho tin nhắn chat history
 */
data class ChatHistoryItem(
    val id: Long,
    val content: String,
    val isFromUser: Boolean,
    val createdAt: LocalDateTime
)

@RestController
@RequestMapping("/api/chat")
class ChatbotController(
    private val chatbotService: ChatbotService,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
    private val encryptionService: EncryptionService
) {

    /**
     * Gửi tin nhắn và nhận phản hồi từ AI chatbot
     * Đồng thời lưu cả tin nhắn user và phản hồi AI vào database
     * 
     * SECURITY:
     * - XSS: Sanitize input message trước khi xử lý
     * - Encryption: Mã hóa tin nhắn trước khi lưu vào database
     * 
     * POST /api/chat
     */
    @PostMapping
    @Transactional
    fun chat(
        @RequestBody request: ChatRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ChatResponse> {
        val userId = getUserIdFromJwt(jwt)
        
        // Step 1: XSS Sanitize input message
        val sanitizedMessage = XssSanitizer.sanitize(request.message)
        
        // Step 2: Encrypt và lưu tin nhắn của user
        val encryptedUserMessage = encryptionService.encrypt(sanitizedMessage)
        chatMessageRepository.save(
            ChatMessageEntity(
                userId = userId,
                content = encryptedUserMessage,  // Lưu tin nhắn đã mã hóa
                isFromUser = true
            )
        )
        
        // Step 3: Gọi AI với tin nhắn đã sanitize (không cần decrypt vì gửi realtime)
        val response = chatbotService.processMessage(
            userMessage = sanitizedMessage,
            conversationId = request.conversationId
        )
        
        // Step 4: Encrypt và lưu phản hồi của AI
        val encryptedAiReply = encryptionService.encrypt(response.reply)
        chatMessageRepository.save(
            ChatMessageEntity(
                userId = userId,
                content = encryptedAiReply,  // Lưu phản hồi đã mã hóa
                isFromUser = false
            )
        )
        
        // Trả về response chưa mã hóa (client cần đọc ngay)
        return ResponseEntity.ok(response)
    }

    /**
     * Lấy lịch sử chat của user hiện tại
     * 
     * SECURITY:
     * - Decrypt: Giải mã tin nhắn trước khi trả về client
     * 
     * GET /api/chat/history
     */
    @GetMapping("/history")
    fun getHistory(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<ChatHistoryItem>> {
        val userId = getUserIdFromJwt(jwt)
        
        val messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)
        
        // Decrypt tin nhắn trước khi trả về
        val history = messages.map { msg ->
            ChatHistoryItem(
                id = msg.id,
                content = encryptionService.safeDecrypt(msg.content),  // Giải mã tin nhắn
                isFromUser = msg.isFromUser,
                createdAt = msg.createdAt
            )
        }
        
        return ResponseEntity.ok(history)
    }

    /**
     * Xóa toàn bộ lịch sử chat của user
     * DELETE /api/chat/history
     */
    @DeleteMapping("/history")
    @Transactional
    fun clearHistory(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Map<String, String>> {
        val userId = getUserIdFromJwt(jwt)
        chatMessageRepository.deleteByUserId(userId)
        
        return ResponseEntity.ok(mapOf(
            "status" to "ok",
            "message" to "Đã xóa lịch sử chat"
        ))
    }

    /**
     * Health check endpoint
     * GET /api/chat/health
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "ok",
            "service" to "Gym AI Chatbot"
        ))
    }

    /**
     * Lấy user ID từ JWT token
     */
    private fun getUserIdFromJwt(jwt: Jwt): Long {
        val email = jwt.subject
        val user = userRepository.findByEmail(email)
            ?: throw RuntimeException("User not found")
        return user.id
    }
}


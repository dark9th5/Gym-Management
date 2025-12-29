package lc._th5.gym_BE.model.chat

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

/**
 * Entity lưu trữ lịch sử tin nhắn chat của mỗi user
 */
@Entity
@Table(
    name = "chat_messages",
    indexes = [
        Index(name = "idx_chat_user_id", columnList = "user_id"),
        Index(name = "idx_chat_created_at", columnList = "created_at")
    ]
)
data class ChatMessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * ID của user sở hữu tin nhắn này
     */
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    /**
     * Nội dung tin nhắn
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    /**
     * true = tin nhắn từ user, false = tin nhắn từ AI
     */
    @Column(name = "is_from_user", nullable = false)
    val isFromUser: Boolean,

    /**
     * Thời gian tạo tin nhắn
     */
    @Column(name = "created_at")
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
)

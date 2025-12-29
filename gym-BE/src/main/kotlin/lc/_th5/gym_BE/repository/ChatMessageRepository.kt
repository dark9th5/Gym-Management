package lc._th5.gym_BE.repository

import lc._th5.gym_BE.model.chat.ChatMessageEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, Long> {

    /**
     * Lấy lịch sử chat của user, sắp xếp theo thời gian mới nhất
     */
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): List<ChatMessageEntity>

    /**
     * Lấy tất cả tin nhắn của user, sắp xếp theo thời gian tăng dần
     */
    fun findByUserIdOrderByCreatedAtAsc(userId: Long): List<ChatMessageEntity>

    /**
     * Đếm số tin nhắn của user
     */
    fun countByUserId(userId: Long): Long

    /**
     * Xóa tất cả tin nhắn của user
     */
    @Modifying
    @Query("DELETE FROM ChatMessageEntity c WHERE c.userId = :userId")
    fun deleteByUserId(userId: Long)
}

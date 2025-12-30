package lc._th5.gym_BE.repository.notification

import lc._th5.gym_BE.model.notification.SystemNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SystemNotificationRepository : JpaRepository<SystemNotification, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<SystemNotification>
}

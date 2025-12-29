package lc._th5.gym_BE.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component

@Component
class MailHealthCheck(
    @Value("\${spring.mail.host:smtp.gmail.com}") private val mailHost: String,
    @Value("\${spring.mail.username:}") private val mailUser: String
) {
    private val logger = LoggerFactory.getLogger(MailHealthCheck::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        logger.info("[MailHealthCheck] spring.mail.host={}", mailHost)
        logger.info("[MailHealthCheck] spring.mail.username={}", if (mailUser.isNotBlank()) "(set)" else "(not set)")

        try {
            // Try to reflectively check JavaMailSenderImpl if available in context
            // This is a best-effort diagnostic; not required for production
            val cls = Class.forName("org.springframework.mail.javamail.JavaMailSenderImpl")
            val bean = org.springframework.context.support.StaticApplicationContext().getBeanFactory()
            // we avoid failing startup; just log that check is available
            logger.info("[MailHealthCheck] JavaMailSenderImpl class available for diagnostics")
        } catch (ex: Exception) {
            logger.debug("[MailHealthCheck] JavaMailSenderImpl not available: {}", ex.message)
        }
    }
}

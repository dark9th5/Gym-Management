package lc._th5.gym_BE.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username:}") private val fromEmail: String
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    fun sendVerificationEmail(to: String, code: String) {
        logger.info("[EmailService] Sending verification email to: {}", to)
        val message = SimpleMailMessage()
        if (fromEmail.isNotBlank()) {
            message.from = fromEmail
        }
        message.setTo(to)
        message.setSubject("Gym App - Mã xác thực email")
        message.setText("""
            Chào mừng bạn đến với Gym App!

            Mã xác thực của bạn là: $code

            Vui lòng nhập mã này để hoàn tất đăng ký tài khoản.

            Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.

            Trân trọng,
            Đội ngũ Gym App
        """.trimIndent())
        mailSender.send(message)
        logger.info("[EmailService] Email sent to: {}", to)
    }
}
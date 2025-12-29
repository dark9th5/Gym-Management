package lc._th5.gym_BE.auth.dto

import lc._th5.gym_BE.model.user.Role

data class AuthResponse(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserInfo,
    // 2FA fields
    val requires2fa: Boolean = false,
    val tempToken: String? = null  // Token tạm dùng để verify 2FA
) {
    data class UserInfo(
        val id: Long,
        val username: String,
        val email: String,
        val fullName: String?,
        val roles: Set<Role>,
        val isVerified: Boolean,
        val is2faEnabled: Boolean = false
    )
}

/**
 * Response khi cần 2FA verification
 */
data class TwoFactorRequiredResponse(
    val requires2fa: Boolean = true,
    val tempToken: String,
    val message: String = "Vui lòng nhập mã xác thực từ ứng dụng Authenticator"
)
package lc._th5.gym_BE.controller

import lc._th5.gym_BE.auth.dto.AuthResponse
import lc._th5.gym_BE.auth.dto.LoginRequest
import lc._th5.gym_BE.auth.dto.RegisterRequest
import lc._th5.gym_BE.auth.dto.TwoFactorRequiredResponse
import lc._th5.gym_BE.service.UserService
import lc._th5.gym_BE.service.TokenService
import lc._th5.gym_BE.service.LoginAttemptService
import lc._th5.gym_BE.service.TotpService
import lc._th5.gym_BE.service.TwoFactorLoginRequest
import lc._th5.gym_BE.util.EncryptedMemoryService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val tokenService: TokenService,
    private val loginAttemptService: LoginAttemptService,
    private val jwtDecoder: JwtDecoder,
    private val totpService: TotpService,
    private val encryptedMemoryService: EncryptedMemoryService
) {
    // Temporary storage cho 2FA pending logins (encrypted in memory)
    private val pending2faLogins = ConcurrentHashMap<String, String>() // encryptedKey -> encryptedData
    
    data class Pending2faLogin(
        val userId: Long,
        val email: String,
        val expiresAt: LocalDateTime
    )

    @PostMapping("/register")
    fun register(@RequestBody @Valid body: RegisterRequest): ResponseEntity<Any> {
        // Verify email code first
        val verifySuccess = userService.verifyEmailCode(body.email, body.code)
        if (!verifySuccess) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Mã xác thực không hợp lệ hoặc đã hết hạn"))
        }
        val user = userService.register(
            body.username,
            body.email,
            body.password,
            body.fullName
        )
        // Generate tokens
        val (token, expiresIn) = tokenService.generateAccessToken(user)
        // Generate refresh token
        val refreshToken = tokenService.generateRefreshToken(user)
        val resp = AuthResponse(
            accessToken = token,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            user = AuthResponse.UserInfo(
                id = user.id,
                username = user.username,
                email = user.email,
                fullName = user.fullName,
                roles = user.roles,
                isVerified = user.isVerified,
                is2faEnabled = user.is2faEnabled
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(resp)
    }

    @PostMapping("/request-email-verification")
    fun requestEmailVerification(@RequestParam email: String): ResponseEntity<Map<String, String>> {
        return try {
            val sent = userService.sendEmailVerification(email)
            if (sent) {
                ResponseEntity.ok(mapOf("message" to "Đã gửi mã xác thực tới email"))
            } else {
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "Không thể gửi mã xác thực"))
            }
        } catch (e: IllegalStateException) {
            // Throttled resend
            ResponseEntity.status(429).body(mapOf("error" to e.message.orEmpty()))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message.orEmpty()))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to e.message.orEmpty()))
        }
    }

    @PostMapping("/verify-email-code")
    fun verifyEmailCode(@RequestParam email: String, @RequestParam code: String): ResponseEntity<Map<String, String>> {
        val success = userService.verifyEmailCode(email, code)
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "Email đã được xác thực thành công"))
        } else {
            ResponseEntity.badRequest().body(mapOf("error" to "Mã xác thực không hợp lệ hoặc email chưa đăng ký"))
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid body: LoginRequest, request: HttpServletRequest): ResponseEntity<Any> {
        val clientIP = getClientIP(request)
        if (loginAttemptService.isBlocked(clientIP)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to "Quá nhiều lần thử đăng nhập. Vui lòng thử lại sau."))
        }

        val identifier = body.username.trim()
        val authToken = UsernamePasswordAuthenticationToken(identifier, body.password)
        val authentication = try {
            authenticationManager.authenticate(authToken)
        } catch (e: Exception) {
            loginAttemptService.loginFailed(clientIP)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Thông tin đăng nhập không đúng"))
        }

        // Success, reset attempts
        loginAttemptService.loginSucceeded(clientIP)

        // Principal is always the user email from UserDetailsService
        val user = userService.findByEmail(authentication.name)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Thông tin đăng nhập không đúng"))

        if (!user.isVerified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Tài khoản chưa được xác thực. Vui lòng kiểm tra email và xác thực tài khoản."))
        }

        // ==================== 2FA CHECK ====================
        if (user.is2faEnabled) {
            // User có 2FA, tạo temp token và yêu cầu verify
            val tempToken = UUID.randomUUID().toString()
            val encryptedTempToken = encryptedMemoryService.encryptKey(tempToken)
            
            val pendingLogin = Pending2faLogin(
                userId = user.id,
                email = user.email,
                expiresAt = LocalDateTime.now().plusMinutes(5) // 5 phút để nhập mã
            )
            val encryptedData = encryptedMemoryService.encryptObject(pendingLogin)
            
            pending2faLogins[encryptedTempToken] = encryptedData
            
            // Cleanup expired tokens
            cleanupExpiredTokens()
            
            return ResponseEntity.ok(TwoFactorRequiredResponse(
                requires2fa = true,
                tempToken = tempToken, // Trả về tempToken gốc cho client
                message = "Vui lòng nhập mã xác thực từ ứng dụng Authenticator"
            ))
        }
        // ==================== END 2FA CHECK ====================

        val (token, expiresIn) = tokenService.generateAccessToken(user)
        val refreshToken = tokenService.generateRefreshToken(user)

        return ResponseEntity.ok(
            AuthResponse(
                accessToken = token,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                user = AuthResponse.UserInfo(
                    id = user.id,
                    username = user.username,
                    email = user.email,
                    fullName = user.fullName,
                    roles = user.roles,
                    isVerified = user.isVerified,
                    is2faEnabled = user.is2faEnabled
                )
            )
        )
    }

    /**
     * Endpoint mới: Verify 2FA code sau khi login
     * User đã nhập username/password đúng, giờ cần nhập mã từ Authenticator
     */
    @PostMapping("/login/2fa")
    fun verify2faLogin(@RequestBody request: TwoFactorLoginRequest): ResponseEntity<Any> {
        val encryptedTempToken = encryptedMemoryService.encryptKey(request.tempToken)
        val encryptedData = pending2faLogins[encryptedTempToken]
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."))
        
        val pendingLogin = encryptedMemoryService.decryptObject(encryptedData, Pending2faLogin::class.java)
        
        // Check expiry
        if (pendingLogin.expiresAt.isBefore(LocalDateTime.now())) {
            pending2faLogins.remove(encryptedTempToken)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."))
        }
        
        val user = userService.findById(pendingLogin.userId)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Không tìm thấy người dùng."))
        
        // Verify TOTP code hoặc backup code
        val isValid = totpService.verifyCode(user, request.code) || 
                      totpService.verifyBackupCode(user.id, request.code)
        
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Mã xác thực không đúng. Vui lòng thử lại."))
        }
        
        // Remove pending login
        pending2faLogins.remove(encryptedTempToken)
        
        // Generate tokens
        val (token, expiresIn) = tokenService.generateAccessToken(user)
        val refreshToken = tokenService.generateRefreshToken(user)
        
        return ResponseEntity.ok(
            AuthResponse(
                accessToken = token,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                user = AuthResponse.UserInfo(
                    id = user.id,
                    username = user.username,
                    email = user.email,
                    fullName = user.fullName,
                    roles = user.roles,
                    isVerified = user.isVerified,
                    is2faEnabled = user.is2faEnabled
                )
            )
        )
    }

    // Refresh token endpoint
    @PostMapping("/refresh")
    fun refresh(@RequestHeader("Authorization") authHeader: String): ResponseEntity<AuthResponse> {
        if (!authHeader.startsWith("Bearer ")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header")
        }
        val refreshToken = authHeader.substring(7)

        val result = tokenService.generateAccessTokenFromRefreshToken(refreshToken)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token")

        val (newAccessToken, expiresIn, newRefreshToken) = result

        // Get user from new refresh token entity
        val storedToken = tokenService.getRefreshTokenEntity(newRefreshToken)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found")

        return ResponseEntity.ok(
            AuthResponse(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresIn = expiresIn,
                user = AuthResponse.UserInfo(
                    id = storedToken.user.id,
                    username = storedToken.user.username,
                    email = storedToken.user.email,
                    fullName = storedToken.user.fullName,
                    roles = storedToken.user.roles,
                    isVerified = storedToken.user.isVerified,
                    is2faEnabled = storedToken.user.is2faEnabled
                )
            )
        )
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Map<String, String>> {
        if (!authHeader.startsWith("Bearer ")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header")
        }
        val token = authHeader.substring(7)

        try {
            val jwt = jwtDecoder.decode(token)
            val jti = jwt.id
            val expiresAt = java.time.LocalDateTime.ofInstant(jwt.expiresAt, java.time.ZoneOffset.UTC)
            tokenService.blacklistToken(jti, expiresAt)
        } catch (e: Exception) {
            // Invalid token, ignore
        }

        return ResponseEntity.ok(mapOf("message" to "Đã đăng xuất thành công"))
    }
    
    // ==================== UPDATE PROFILE ====================
    
    data class UpdateProfileRequest(
        val fullName: String? = null,
        val username: String? = null
    )
    
    @PutMapping("/profile")
    fun updateProfile(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<Any> {
        if (!authHeader.startsWith("Bearer ")) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header")
        }
        val token = authHeader.substring(7)
        
        try {
            val jwt = jwtDecoder.decode(token)
            val userId = jwt.subject.toLongOrNull()
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")
            
            val user = userService.findById(userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            
            // Update fields
            val updatedUser = userService.updateProfile(
                userId = userId,
                fullName = request.fullName,
                newUsername = request.username
            )
            
            return ResponseEntity.ok(mapOf(
                "message" to "Cập nhật hồ sơ thành công",
                "user" to AuthResponse.UserInfo(
                    id = updatedUser.id,
                    username = updatedUser.username,
                    email = updatedUser.email,
                    fullName = updatedUser.fullName,
                    roles = updatedUser.roles,
                    isVerified = updatedUser.isVerified,
                    is2faEnabled = updatedUser.is2faEnabled
                )
            ))
        } catch (e: ResponseStatusException) {
            throw e
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message ?: "Cập nhật thất bại")
        }
    }
    
    // ==================== CHANGE PASSWORD ====================
    
    data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
        val twoFactorCode: String? = null  // Required if 2FA is enabled
    )
    
    @PostMapping("/change-password")
    fun changePassword(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Any> {
        if (!authHeader.startsWith("Bearer ")) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header")
        }
        val token = authHeader.substring(7)
        
        try {
            val jwt = jwtDecoder.decode(token)
            val userId = jwt.subject.toLongOrNull()
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")
            
            val user = userService.findById(userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            
            // Verify 2FA if enabled
            if (user.is2faEnabled) {
                if (request.twoFactorCode.isNullOrBlank()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(mapOf(
                            "error" to "Yêu cầu mã xác thực 2FA",
                            "requires2fa" to true
                        ))
                }
                
                // Verify TOTP code or backup code
                val isValidTotp = totpService.verifyCode(user, request.twoFactorCode)
                val isValidBackup = if (!isValidTotp) {
                    totpService.verifyBackupCode(userId, request.twoFactorCode)
                } else false
                
                if (!isValidTotp && !isValidBackup) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(mapOf("error" to "Mã xác thực không đúng"))
                }
            }
            
            // Change password
            userService.changePassword(userId, request.currentPassword, request.newPassword)
            
            return ResponseEntity.ok(mapOf("message" to "Đổi mật khẩu thành công"))
        } catch (e: ResponseStatusException) {
            throw e
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to (e.message ?: "Mật khẩu hiện tại không đúng")))
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message ?: "Đổi mật khẩu thất bại")
        }
    }

    private fun getClientIP(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (xForwardedFor != null && xForwardedFor.isNotEmpty()) {
            xForwardedFor.split(",")[0].trim()
        } else {
            request.remoteAddr ?: "unknown"
        }
    }
    
    private fun cleanupExpiredTokens() {
        val now = LocalDateTime.now()
        pending2faLogins.entries.removeIf { entry ->
            try {
                val pendingLogin = encryptedMemoryService.decryptObject(entry.value, Pending2faLogin::class.java)
                pendingLogin.expiresAt.isBefore(now)
            } catch (e: Exception) {
                // If decryption fails, remove the entry
                true
            }
        }
    }
}
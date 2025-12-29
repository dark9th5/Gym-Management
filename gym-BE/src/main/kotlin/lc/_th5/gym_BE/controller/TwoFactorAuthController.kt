package lc._th5.gym_BE.controller

import lc._th5.gym_BE.service.TotpService
import lc._th5.gym_BE.service.TwoFactorSetupResponse
import lc._th5.gym_BE.service.TwoFactorStatusResponse
import lc._th5.gym_BE.service.TwoFactorVerifyRequest
import lc._th5.gym_BE.service.TwoFactorVerifyResponse
import lc._th5.gym_BE.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

/**
 * Controller xử lý 2FA (Two-Factor Authentication) với TOTP
 * 
 * Flow:
 * 1. GET  /api/2fa/status  - Kiểm tra trạng thái 2FA
 * 2. POST /api/2fa/setup   - Khởi tạo 2FA, nhận QR code
 * 3. POST /api/2fa/verify  - Xác nhận enable 2FA
 * 4. POST /api/2fa/disable - Tắt 2FA
 * 5. POST /api/2fa/backup/regenerate - Tạo backup codes mới
 */
@RestController
@RequestMapping("/api/2fa")
class TwoFactorAuthController(
    private val totpService: TotpService,
    private val userService: UserService
) {
    
    /**
     * Lấy trạng thái 2FA hiện tại của user
     */
    @GetMapping("/status")
    fun get2faStatus(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<TwoFactorStatusResponse> {
        val userId = jwt.getClaim<Long>("uid")
        val status = totpService.get2faStatus(userId)
        return ResponseEntity.ok(status)
    }
    
    /**
     * Bước 1: Khởi tạo setup 2FA
     * Trả về secret và QR code để scan bằng Google Authenticator
     */
    @PostMapping("/setup")
    fun setup2fa(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Any> {
        return try {
            val userId = jwt.getClaim<Long>("uid")
            val setupData = totpService.initiate2faSetup(userId)
            ResponseEntity.ok(setupData)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
    
    /**
     * Bước 2: Xác nhận và enable 2FA
     * User nhập mã từ Authenticator app để xác nhận đã setup đúng
     */
    @PostMapping("/verify")
    fun verify2fa(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: TwoFactorVerifyRequest
    ): ResponseEntity<TwoFactorVerifyResponse> {
        return try {
            val userId = jwt.getClaim<Long>("uid")
            val result = totpService.verify2faSetup(userId, request.code)
            
            if (result.success) {
                ResponseEntity.ok(result)
            } else {
                ResponseEntity.badRequest().body(result)
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(
                TwoFactorVerifyResponse(
                    success = false,
                    message = e.message ?: "Lỗi không xác định",
                    backupCodes = null
                )
            )
        }
    }
    
    /**
     * Tắt 2FA
     * Yêu cầu nhập mã TOTP hoặc backup code để xác nhận
     */
    @PostMapping("/disable")
    fun disable2fa(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: TwoFactorVerifyRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val userId = jwt.getClaim<Long>("uid")
            val success = totpService.disable2fa(userId, request.code)
            
            if (success) {
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "2FA đã được tắt thành công"
                ))
            } else {
                ResponseEntity.badRequest().body(mapOf(
                    "success" to false,
                    "error" to "Mã xác thực không đúng"
                ))
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "error" to (e.message ?: "Lỗi không xác định")
            ))
        }
    }
    
    /**
     * Tạo backup codes mới
     * Yêu cầu xác thực bằng mã TOTP hiện tại
     */
    @PostMapping("/backup/regenerate")
    fun regenerateBackupCodes(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: TwoFactorVerifyRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val userId = jwt.getClaim<Long>("uid")
            val newCodes = totpService.regenerateBackupCodes(userId, request.code)
            
            if (newCodes != null) {
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "Đã tạo backup codes mới",
                    "backupCodes" to newCodes
                ))
            } else {
                ResponseEntity.badRequest().body(mapOf(
                    "success" to false,
                    "error" to "Mã xác thực không đúng"
                ))
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "error" to (e.message ?: "Lỗi không xác định")
            ))
        }
    }
}

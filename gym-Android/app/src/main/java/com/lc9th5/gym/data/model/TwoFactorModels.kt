package com.lc9th5.gym.data.model

import com.google.gson.annotations.SerializedName

// ==================== 2FA Response Models ====================

/**
 * Response khi login yêu cầu 2FA
 */
data class TwoFactorRequiredResponse(
    @SerializedName("requires2fa") val requires2fa: Boolean = true,
    @SerializedName("tempToken") val tempToken: String,
    val message: String
)

/**
 * Response từ /2fa/setup - chứa QR code để scan
 */
data class TwoFactorSetupResponse(
    val secret: String,
    @SerializedName("qrCodeBase64") val qrCodeBase64: String?,
    @SerializedName("manualEntryKey") val manualEntryKey: String,
    val issuer: String,
    @SerializedName("accountName") val accountName: String
)

/**
 * Response từ /2fa/verify - kết quả enable 2FA
 */
data class TwoFactorVerifyResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("backupCodes") val backupCodes: List<String>?
)

/**
 * Response từ /2fa/status
 */
data class TwoFactorStatusResponse(
    @SerializedName("is2faEnabled") val is2faEnabled: Boolean,
    @SerializedName("hasSecret") val hasSecret: Boolean,
    @SerializedName("backupCodesRemaining") val backupCodesRemaining: Int
)

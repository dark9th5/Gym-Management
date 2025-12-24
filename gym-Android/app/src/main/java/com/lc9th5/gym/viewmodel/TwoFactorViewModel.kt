package com.lc9th5.gym.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.model.TwoFactorSetupResponse
import com.lc9th5.gym.data.model.TwoFactorStatusResponse
import com.lc9th5.gym.data.repository.TwoFactorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel xử lý logic 2FA
 */
class TwoFactorViewModel(
    private val twoFactorRepository: TwoFactorRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(TwoFactorUiState())
    val uiState: StateFlow<TwoFactorUiState> = _uiState.asStateFlow()
    
    // Setup response (chứa QR code)
    private val _setupResponse = MutableStateFlow<TwoFactorSetupResponse?>(null)
    val setupResponse: StateFlow<TwoFactorSetupResponse?> = _setupResponse.asStateFlow()
    
    // QR Code bitmap
    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap.asStateFlow()
    
    // Backup codes sau khi enable 2FA
    private val _backupCodes = MutableStateFlow<List<String>>(emptyList())
    val backupCodes: StateFlow<List<String>> = _backupCodes.asStateFlow()
    
    init {
        load2faStatus()
    }
    
    /**
     * Load trạng thái 2FA hiện tại
     */
    fun load2faStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            twoFactorRepository.get2faStatus().fold(
                onSuccess = { status ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        is2faEnabled = status.is2faEnabled,
                        backupCodesRemaining = status.backupCodesRemaining
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    /**
     * Bước 1: Khởi tạo setup 2FA
     */
    fun initiateSetup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            twoFactorRepository.setup2fa().fold(
                onSuccess = { response ->
                    _setupResponse.value = response
                    
                    // Decode QR code base64 to bitmap
                    response.qrCodeBase64?.let { base64 ->
                        try {
                            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            _qrCodeBitmap.value = bitmap
                        } catch (e: Exception) {
                            // QR code decode failed, user can use manual entry
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        setupStep = SetupStep.SCAN_QR
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    /**
     * Bước 2: Verify và enable 2FA
     */
    fun verifyAndEnable(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            twoFactorRepository.verify2fa(code).fold(
                onSuccess = { response ->
                    if (response.success) {
                        _backupCodes.value = response.backupCodes ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            is2faEnabled = true,
                            setupStep = SetupStep.SHOW_BACKUP_CODES,
                            successMessage = response.message
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    /**
     * Tắt 2FA
     */
    fun disable2fa(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            twoFactorRepository.disable2fa(code).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        is2faEnabled = false,
                        setupStep = SetupStep.INITIAL,
                        successMessage = "2FA đã được tắt thành công"
                    )
                    // Clear setup data
                    _setupResponse.value = null
                    _qrCodeBitmap.value = null
                    _backupCodes.value = emptyList()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    /**
     * Tạo backup codes mới
     */
    fun regenerateBackupCodes(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            twoFactorRepository.regenerateBackupCodes(code).fold(
                onSuccess = { newCodes ->
                    _backupCodes.value = newCodes
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        backupCodesRemaining = newCodes.size,
                        successMessage = "Đã tạo ${newCodes.size} backup codes mới"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    /**
     * Reset về trạng thái ban đầu
     */
    fun resetSetup() {
        _setupResponse.value = null
        _qrCodeBitmap.value = null
        _backupCodes.value = emptyList()
        _uiState.value = _uiState.value.copy(
            setupStep = SetupStep.INITIAL,
            error = null,
            successMessage = null
        )
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    class Factory(
        private val twoFactorRepository: TwoFactorRepository,
        private val tokenManager: TokenManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TwoFactorViewModel(twoFactorRepository, tokenManager) as T
        }
    }
}

/**
 * UI State cho 2FA screen
 */
data class TwoFactorUiState(
    val isLoading: Boolean = false,
    val is2faEnabled: Boolean = false,
    val backupCodesRemaining: Int = 0,
    val setupStep: SetupStep = SetupStep.INITIAL,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * Các bước trong quá trình setup 2FA
 */
enum class SetupStep {
    INITIAL,        // Chưa bắt đầu setup
    SCAN_QR,        // Hiển thị QR code để scan
    ENTER_CODE,     // Nhập mã từ Authenticator
    SHOW_BACKUP_CODES,  // Hiển thị backup codes
    COMPLETED       // Hoàn tất
}

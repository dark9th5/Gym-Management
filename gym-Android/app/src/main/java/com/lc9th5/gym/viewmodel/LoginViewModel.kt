package com.lc9th5.gym.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.LoginRequest
import com.lc9th5.gym.data.repository.AuthRepository
import com.lc9th5.gym.data.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository, private val context: Context) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val tokenManager = TokenManager(context)
    
    // Encrypted storage cho temporary 2FA token
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "login_temp_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private companion object {
        private const val KEY_PENDING_2FA_TOKEN = "pending_2fa_token"
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            when (val result = repository.login(LoginRequest(username.trim(), password))) {
                is LoginResult.Success -> {
                    // Login thành công, lưu tokens
                    tokenManager.saveAuthResponse(result.response)
                    _loginState.value = LoginState.Success(result.response)
                }
                is LoginResult.Requires2FA -> {
                    // Cần 2FA verification - lưu encrypted token
                    encryptedPrefs.edit().putString(KEY_PENDING_2FA_TOKEN, result.tempToken).apply()
                    _loginState.value = LoginState.Requires2FA(result.tempToken, result.message)
                }
                is LoginResult.Error -> {
                    _loginState.value = LoginState.Error(result.message)
                }
            }
        }
    }
    
    /**
     * Verify 2FA code
     */
    fun verify2FA(code: String) {
        val tempToken = encryptedPrefs.getString(KEY_PENDING_2FA_TOKEN, null)
        if (tempToken == null) {
            _loginState.value = LoginState.Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
            return
        }
        
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            val result = repository.verify2FALogin(tempToken, code)
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                // Lưu auth response
                tokenManager.saveAuthResponse(response)
                // Clear encrypted temp token
                encryptedPrefs.edit().remove(KEY_PENDING_2FA_TOKEN).apply()
                _loginState.value = LoginState.Success(response)
            } else {
                _loginState.value = LoginState.TwoFAError(
                    tempToken, 
                    result.exceptionOrNull()?.message ?: "Mã xác thực không đúng"
                )
            }
        }
    }
    
    /**
     * Cancel 2FA và quay lại login
     */
    fun cancel2FA() {
        encryptedPrefs.edit().remove(KEY_PENDING_2FA_TOKEN).apply()
        _loginState.value = LoginState.Idle
    }
    
    /**
     * Reset state về Idle
     */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val response: AuthResponse) : LoginState()
    data class Error(val message: String) : LoginState()
    
    // 2FA States
    data class Requires2FA(val tempToken: String, val message: String) : LoginState()
    data class TwoFAError(val tempToken: String, val message: String) : LoginState()
}

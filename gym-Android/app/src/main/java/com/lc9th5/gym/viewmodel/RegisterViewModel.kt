package com.lc9th5.gym.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.RegisterRequest
import com.lc9th5.gym.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    private val _emailVerifyState = MutableStateFlow<EmailVerifyState>(EmailVerifyState.Idle)
    val emailVerifyState: StateFlow<EmailVerifyState> = _emailVerifyState

    fun register(username: String, email: String, password: String, fullName: String, code: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            val result = repository.register(RegisterRequest(username, email.trim().lowercase(), password, fullName, code))
            if (result.isSuccess) {
                _registerState.value = RegisterState.Success(result.getOrNull()!!)
            } else {
                _registerState.value = RegisterState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun requestEmailVerification(email: String) {
        viewModelScope.launch {
            _emailVerifyState.value = EmailVerifyState.Loading
            val result = repository.requestEmailVerification(email)
            if (result.isSuccess) {
                _emailVerifyState.value = EmailVerifyState.Success(result.getOrNull() ?: "Đã gửi mã xác thực")
            } else {
                _emailVerifyState.value = EmailVerifyState.Error(result.exceptionOrNull()?.message ?: "Gửi mã xác thực thất bại")
            }
        }
    }

    fun verifyEmailCode(email: String, code: String) {
        viewModelScope.launch {
            _emailVerifyState.value = EmailVerifyState.Loading
            val result = repository.verifyEmailCode(email, code)
            if (result.isSuccess) {
                _emailVerifyState.value = EmailVerifyState.Success(result.getOrNull() ?: "Email đã được xác thực")
            } else {
                _emailVerifyState.value = EmailVerifyState.Error(result.exceptionOrNull()?.message ?: "Xác thực email thất bại")
            }
        }
    }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val response: AuthResponse) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

sealed class EmailVerifyState {
    object Idle : EmailVerifyState()
    object Loading : EmailVerifyState()
    data class Success(val message: String) : EmailVerifyState()
    data class Error(val message: String) : EmailVerifyState()
}

package com.lc9th5.gym.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.LoginRequest
import com.lc9th5.gym.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository, private val context: Context) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val tokenManager = TokenManager(context)

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = repository.login(LoginRequest(username.trim(), password))
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                // Save auth response to TokenManager
                tokenManager.saveAuthResponse(response)
                _loginState.value = LoginState.Success(response)
            } else {
                _loginState.value = LoginState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val response: AuthResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}

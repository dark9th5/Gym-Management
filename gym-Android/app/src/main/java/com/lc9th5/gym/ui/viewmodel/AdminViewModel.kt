package com.lc9th5.gym.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lc9th5.gym.data.model.AdminDashboardStats
import com.lc9th5.gym.data.model.UserSummary
import com.lc9th5.gym.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: AdminRepository) : ViewModel() {

    // Dashboard State
    private val _dashboardStats = MutableStateFlow<AdminDashboardStats?>(null)
    val dashboardStats: StateFlow<AdminDashboardStats?> = _dashboardStats.asStateFlow()

    // Users List State
    private val _users = MutableStateFlow<List<UserSummary>>(emptyList())
    val users: StateFlow<List<UserSummary>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _notificationStatus = MutableStateFlow<String?>(null)
    val notificationStatus: StateFlow<String?> = _notificationStatus.asStateFlow()

    fun loadDashboardStats() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDashboardStats()
                .onSuccess { stats ->
                    _dashboardStats.value = stats
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            _isLoading.value = false
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllUsers()
                .onSuccess { userList ->
                    _users.value = userList
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            _isLoading.value = false
        }
    }

    fun toggleUserLock(user: UserSummary) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (user.isLocked) {
                repository.unlockUser(user.id)
            } else {
                repository.lockUser(user.id, "Admin locked via App")
            }

            result.onSuccess { updatedUser ->
                // Update local list to reflect changes
                _users.value = _users.value.map {
                    if (it.id == user.id) updatedUser else it
                }
            }.onFailure { exception ->
                _error.value = exception.message
            }
            _isLoading.value = false
        }
    }

    fun sendNotification(title: String, content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.sendNotification(title, content)
                .onSuccess {
                    _notificationStatus.value = "Notification Sent!"
                }
                .onFailure { exception ->
                    _notificationStatus.value = "Failed: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    
    fun clearNotificationStatus() {
        _notificationStatus.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
}

class AdminViewModelFactory(private val repository: AdminRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

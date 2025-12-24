package com.lc9th5.gym.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lc9th5.gym.data.model.ChatMessage
import com.lc9th5.gym.data.model.ChatRequest
import com.lc9th5.gym.data.remote.ChatApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val error: String? = null,
    val conversationId: String? = null
)

class ChatViewModel(
    private val chatApiService: ChatApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val welcomeMessage = ChatMessage(
        id = "welcome",
        content = "Xin chào! 👋 Tôi là Gym AI Assistant. Hãy hỏi tôi bất cứ điều gì về:\n\n" +
                "💪 Bài tập thể hình\n" +
                "🏃 Cardio & giảm cân\n" +
                "🥗 Dinh dưỡng thể thao\n" +
                "📋 Kế hoạch tập luyện\n\n" +
                "Tôi sẵn sàng hỗ trợ bạn!",
        isFromUser = false,
        timestamp = LocalDateTime.now()
    )

    init {
        // Load lịch sử chat từ server khi khởi tạo
        loadChatHistory()
    }

    /**
     * Load lịch sử chat từ server
     */
    fun loadChatHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            
            try {
                val response = chatApiService.getHistory()
                
                if (response.isSuccessful && response.body() != null) {
                    val historyItems = response.body()!!
                    
                    if (historyItems.isEmpty()) {
                        // Không có lịch sử -> hiển thị welcome message
                        _uiState.update { 
                            it.copy(
                                messages = listOf(welcomeMessage),
                                isLoadingHistory = false
                            ) 
                        }
                    } else {
                        // Có lịch sử -> convert và hiển thị
                        val messages = historyItems.map { item ->
                            ChatMessage(
                                id = item.id.toString(),
                                content = item.content,
                                isFromUser = item.isFromUser,
                                timestamp = try {
                                    LocalDateTime.parse(item.createdAt, DateTimeFormatter.ISO_DATE_TIME)
                                } catch (e: Exception) {
                                    LocalDateTime.now()
                                }
                            )
                        }
                        
                        _uiState.update { 
                            it.copy(
                                messages = messages,
                                isLoadingHistory = false
                            ) 
                        }
                    }
                } else {
                    // Lỗi -> hiển thị welcome message
                    _uiState.update { 
                        it.copy(
                            messages = listOf(welcomeMessage),
                            isLoadingHistory = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                // Lỗi kết nối -> hiển thị welcome message
                _uiState.update { 
                    it.copy(
                        messages = listOf(welcomeMessage),
                        isLoadingHistory = false
                    ) 
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // Thêm tin nhắn người dùng vào UI ngay lập tức
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content.trim(),
            isFromUser = true,
            timestamp = LocalDateTime.now()
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isLoading = true,
                error = null
            )
        }

        // Gọi API
        viewModelScope.launch {
            try {
                val request = ChatRequest(
                    message = content.trim(),
                    conversationId = _uiState.value.conversationId
                )

                val response = chatApiService.sendMessage(request)

                if (response.isSuccessful && response.body() != null) {
                    val chatResponse = response.body()!!
                    val botMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = chatResponse.reply,
                        isFromUser = false,
                        timestamp = LocalDateTime.now()
                    )

                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + botMessage,
                            isLoading = false,
                            conversationId = chatResponse.conversationId
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = "Không thể gửi tin nhắn. Vui lòng thử lại."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Lỗi kết nối: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Xóa lịch sử chat (cả local và server)
     */
    fun clearChat() {
        viewModelScope.launch {
            try {
                // Xóa trên server
                chatApiService.clearHistory()
            } catch (e: Exception) {
                // Ignore error
            }
        }
        
        // Xóa local và hiển thị welcome message
        _uiState.update { 
            ChatUiState(messages = listOf(welcomeMessage)) 
        }
    }

    class Factory(private val chatApiService: ChatApiService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(chatApiService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

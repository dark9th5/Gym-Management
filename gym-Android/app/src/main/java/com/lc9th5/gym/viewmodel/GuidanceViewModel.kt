package com.lc9th5.gym.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.model.GuidanceCategory
import com.lc9th5.gym.data.model.GuidanceLesson
import com.lc9th5.gym.data.model.GuidanceLessonDetail
import com.lc9th5.gym.data.remote.ApiClient
import com.lc9th5.gym.data.repository.GuidanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GuidanceViewModel(private val repository: GuidanceRepository) : ViewModel() {

    private val fallbackCategories = listOf(
        GuidanceCategory(id = 1L, displayName = "Ngực", slug = "chest"),
        GuidanceCategory(id = 2L, displayName = "Vai", slug = "shoulders"),
        GuidanceCategory(id = 3L, displayName = "Tay sau", slug = "triceps"),
        GuidanceCategory(id = 4L, displayName = "Lưng", slug = "back"),
        GuidanceCategory(id = 5L, displayName = "Bụng", slug = "abs"),
        GuidanceCategory(id = 6L, displayName = "Tay trước", slug = "biceps"),
        GuidanceCategory(id = 7L, displayName = "Chân", slug = "legs")
    )

    private val _uiState = MutableStateFlow(
        GuidanceUiState(
            categories = fallbackCategories,
            selectedCategoryId = fallbackCategories.first().id
        )
    )
    val uiState: StateFlow<GuidanceUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun selectCategory(categoryId: Long) {
        // Luôn tải lại bài học ngay cả khi ID trùng (phòng trường hợp backend trả về nhiều nhóm cơ có cùng ID hoặc dữ liệu bị cache sai)
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                lessons = emptyList(),
                lessonsError = null
            )
        }
        loadLessons(categoryId)
    }

    fun openLesson(lesson: GuidanceLesson, number: Int) {
        _uiState.update {
            it.copy(
                isDetailVisible = true,
                isDetailLoading = true,
                detailError = null,
                selectedLessonNumber = number,
                selectedLessonTitle = lesson.title,
                selectedLessonDetail = null
            )
        }
        viewModelScope.launch {
            val result = repository.getLessonDetail(lesson.id)
            result.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        selectedLessonDetail = detail
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        detailError = throwable.message ?: "Không thể tải nội dung bài học"
                    )
                }
            }
        }
    }

    fun closeLessonDetail() {
        _uiState.update {
            it.copy(
                isDetailVisible = false,
                selectedLessonDetail = null,
                selectedLessonNumber = null,
                selectedLessonTitle = null,
                detailError = null
            )
        }
    }

    fun retryLessons() {
        _uiState.value.selectedCategoryId?.let { loadLessons(it) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCategoriesLoading = true, categoryError = null) }
            val result = repository.getCategories()
            val categories = result.getOrElse { fallbackCategories }
            val selected = categories.firstOrNull() ?: fallbackCategories.first()
            _uiState.update {
                it.copy(
                    categories = categories,
                    selectedCategoryId = selected.id,
                    isCategoriesLoading = false,
                    categoryError = result.exceptionOrNull()?.message
                )
            }
            loadLessons(selected.id)
        }
    }

    private fun loadLessons(categoryId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLessonsLoading = true, lessonsError = null) }
            val result = repository.getLessons(categoryId)
            result.onSuccess { lessons ->
                _uiState.update {
                    it.copy(
                        isLessonsLoading = false,
                        lessons = lessons
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLessonsLoading = false,
                        lessonsError = throwable.message ?: "Không thể tải danh sách bài học"
                    )
                }
            }
        }
    }
}

data class GuidanceUiState(
    val categories: List<GuidanceCategory> = emptyList(),
    val selectedCategoryId: Long? = null,
    val lessons: List<GuidanceLesson> = emptyList(),
    val isCategoriesLoading: Boolean = false,
    val isLessonsLoading: Boolean = false,
    val isDetailLoading: Boolean = false,
    val categoryError: String? = null,
    val lessonsError: String? = null,
    val detailError: String? = null,
    val selectedLessonNumber: Int? = null,
    val selectedLessonTitle: String? = null,
    val selectedLessonDetail: GuidanceLessonDetail? = null,
    val isDetailVisible: Boolean = false
)

class GuidanceViewModelFactory(private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GuidanceViewModel::class.java)) {
            val apiClient = ApiClient.getInstance(tokenManager)
            val repository = GuidanceRepository(apiClient.guidanceApiService)
            return GuidanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

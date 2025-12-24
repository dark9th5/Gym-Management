package com.lc9th5.gym.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.model.CreateLessonRequest
import com.lc9th5.gym.data.model.GuidanceCategory
import com.lc9th5.gym.data.model.GuidanceLesson
import com.lc9th5.gym.data.model.GuidanceLessonDetail
import com.lc9th5.gym.data.model.UpdateLessonRequest
import com.lc9th5.gym.data.remote.ApiClient
import com.lc9th5.gym.data.repository.GuidanceRepository
import com.lc9th5.gym.utils.SupabaseStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GuidanceViewModel(
    private val repository: GuidanceRepository,
    private val supabaseStorageHelper: SupabaseStorageHelper
) : ViewModel() {

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
                selectedLesson = lesson,
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
                selectedLesson = null,
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

    fun showAddLesson() {
        _uiState.update {
            it.copy(
                isAddLessonVisible = true,
                createLessonError = null
            )
        }
    }

    fun hideAddLesson() {
        _uiState.update {
            it.copy(
                isAddLessonVisible = false,
                createLessonError = null
            )
        }
    }

    fun showEditLesson(lesson: GuidanceLesson) {
        _uiState.update {
            it.copy(
                isEditLessonVisible = true,
                editingLesson = lesson,
                updateLessonError = null
            )
        }
        // Load detail for editing
        viewModelScope.launch {
            val result = repository.getLessonDetail(lesson.id)
            result.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        editingLessonDetail = detail
                    )
                }
            }.onFailure { throwable ->
                // Handle error, maybe show message
            }
        }
    }

    fun hideEditLesson() {
        _uiState.update {
            it.copy(
                isEditLessonVisible = false,
                editingLesson = null,
                editingLessonDetail = null,
                updateLessonError = null
            )
        }
    }

    fun showConfirmDelete(lesson: GuidanceLesson) {
        _uiState.update {
            it.copy(
                isConfirmDeleteVisible = true,
                lessonToDelete = lesson
            )
        }
    }

    fun hideConfirmDelete() {
        _uiState.update {
            it.copy(
                isConfirmDeleteVisible = false,
                lessonToDelete = null
            )
        }
    }

    fun updateLesson(
        lessonId: Long,
        title: String,
        content: String,
        videoUri: android.net.Uri?,
        imageUri: android.net.Uri?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingLesson = true, updateLessonError = null) }

            try {
                var videoUrl: String? = null
                var imageUrl: String? = null

                // Upload video if provided
                videoUri?.let { uri ->
                    val fileName = "video_${System.currentTimeMillis()}.mp4"
                    videoUrl = supabaseStorageHelper.uploadVideo(uri, fileName)
                }

                // Upload image if provided
                imageUri?.let { uri ->
                    val fileName = "image_${System.currentTimeMillis()}.jpg"
                    imageUrl = supabaseStorageHelper.uploadImage(uri, fileName)
                }

                val request = UpdateLessonRequest(
                    title = title,
                    content = content,
                    videoUrl = videoUrl,
                    imageUrl = imageUrl
                )

                val result = repository.updateLesson(lessonId, request)
                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            isUpdatingLesson = false,
                            isEditLessonVisible = false,
                            editingLesson = null
                        )
                    }
                    // Reload lessons to show the updated one
                    _uiState.value.selectedCategoryId?.let { loadLessons(it) }
                    closeLessonDetail()
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isUpdatingLesson = false,
                            updateLessonError = throwable.message ?: "Không thể cập nhật bài học"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingLesson = false,
                        updateLessonError = e.message ?: "Lỗi không xác định"
                    )
                }
            }
        }
    }

    fun confirmDeleteLesson() {
        val lesson = _uiState.value.lessonToDelete ?: return
        hideConfirmDelete()
        deleteLesson(lesson.id)
    }

    fun deleteLesson(lessonId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingLesson = true) }

            val result = repository.deleteLesson(lessonId)
            result.onSuccess {
                _uiState.update { it.copy(isDeletingLesson = false) }
                // Reload lessons to remove the deleted one
                _uiState.value.selectedCategoryId?.let { loadLessons(it) }
                closeLessonDetail()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isDeletingLesson = false,
                        deleteLessonError = throwable.message ?: "Không thể xóa bài học"
                    )
                }
            }
        }
    }

    fun createLesson(
        categoryId: Long,
        title: String,
        content: String,
        videoUri: android.net.Uri?,
        imageUri: android.net.Uri?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingLesson = true, createLessonError = null) }

            try {
                var videoUrl: String? = null
                var imageUrl: String? = null

                // Upload video if provided
                videoUri?.let { uri ->
                    val fileName = "video_${System.currentTimeMillis()}.mp4"
                    videoUrl = supabaseStorageHelper.uploadVideo(uri, fileName)
                }

                // Upload image if provided
                imageUri?.let { uri ->
                    val fileName = "image_${System.currentTimeMillis()}.jpg"
                    imageUrl = supabaseStorageHelper.uploadImage(uri, fileName)
                }

                val request = CreateLessonRequest(
                    categoryId = categoryId,
                    title = title,
                    content = content,
                    videoUrl = videoUrl,
                    imageUrl = imageUrl
                )

                val result = repository.createLesson(request)
                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            isCreatingLesson = false,
                            isAddLessonVisible = false
                        )
                    }
                    // Reload lessons to show the new one
                    _uiState.value.selectedCategoryId?.let { loadLessons(it) }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreatingLesson = false,
                            createLessonError = throwable.message ?: "Không thể tạo bài học"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingLesson = false,
                        createLessonError = e.message ?: "Lỗi không xác định"
                    )
                }
            }
        }
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
    val selectedLesson: GuidanceLesson? = null,
    val selectedLessonNumber: Int? = null,
    val selectedLessonTitle: String? = null,
    val selectedLessonDetail: GuidanceLessonDetail? = null,
    val isDetailVisible: Boolean = false,
    val isAddLessonVisible: Boolean = false,
    val isCreatingLesson: Boolean = false,
    val createLessonError: String? = null,
    val isEditLessonVisible: Boolean = false,
    val editingLesson: GuidanceLesson? = null,
    val editingLessonDetail: GuidanceLessonDetail? = null,
    val isUpdatingLesson: Boolean = false,
    val updateLessonError: String? = null,
    val isDeletingLesson: Boolean = false,
    val deleteLessonError: String? = null,
    val isConfirmDeleteVisible: Boolean = false,
    val lessonToDelete: GuidanceLesson? = null
)

class GuidanceViewModelFactory(
    private val tokenManager: TokenManager,
    private val supabaseStorageHelper: SupabaseStorageHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GuidanceViewModel::class.java)) {
            val apiClient = ApiClient.getInstance(tokenManager)
            val repository = GuidanceRepository(apiClient.guidanceApiService)
            return GuidanceViewModel(repository, supabaseStorageHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

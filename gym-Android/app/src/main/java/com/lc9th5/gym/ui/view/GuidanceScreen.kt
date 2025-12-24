package com.lc9th5.gym.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import android.Manifest
import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.media3.ui.PlayerView
import com.lc9th5.gym.data.model.GuidanceCategory
import com.lc9th5.gym.data.model.GuidanceLesson
import com.lc9th5.gym.data.model.GuidanceLessonDetail
import com.lc9th5.gym.viewmodel.GuidanceUiState
import com.lc9th5.gym.viewmodel.GuidanceViewModel

@Composable
fun GuidanceScreen(
	modifier: Modifier = Modifier,
	viewModel: GuidanceViewModel,
	isAdmin: Boolean = false // TODO: Pass from parent
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (!uiState.isDetailVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Category row with optional admin add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Empty spacer to maintain layout
                    Spacer(modifier = Modifier.width(1.dp))
                    if (isAdmin) {
                        IconButton(onClick = { viewModel.showAddLesson() }) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm bài học")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                CategoryRow(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    isLoading = uiState.isCategoriesLoading,
                    error = uiState.categoryError,
                    onCategorySelected = { category ->
                        viewModel.selectCategory(category.id)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                LessonsGridSection(
                    uiState = uiState,
                    onLessonSelected = { lesson, number ->
                        viewModel.openLesson(lesson, number)
                    },
                    onRetry = { viewModel.retryLessons() }
                )
            }
        }

        // Dùng Dialog toàn màn hình cho chi tiết bài để giảm overdraw
        if (uiState.isDetailVisible) {
            LessonDetailDialog(
                uiState = uiState,
                onDismiss = viewModel::closeLessonDetail,
                viewModel = viewModel,
                isAdmin = isAdmin
            )
        }

        if (uiState.isAddLessonVisible) {
            AddLessonDialog(
                uiState = uiState,
                categories = uiState.categories,
                onDismiss = viewModel::hideAddLesson,
                onCreateLesson = { categoryId, title, content, videoUri, imageUri ->
                    viewModel.createLesson(categoryId, title, content, videoUri, imageUri)
                }
            )
        }

        if (uiState.isEditLessonVisible) {
            EditLessonDialog(
                uiState = uiState,
                onDismiss = viewModel::hideEditLesson,
                onUpdateLesson = { lessonId, title, content, videoUri, imageUri ->
                    viewModel.updateLesson(lessonId, title, content, videoUri, imageUri)
                }
            )
        }

        if (uiState.isConfirmDeleteVisible) {
            ConfirmDeleteDialog(
                lesson = uiState.lessonToDelete,
                onDismiss = viewModel::hideConfirmDelete,
                onConfirm = viewModel::confirmDeleteLesson,
                isDeleting = uiState.isDeletingLesson
            )
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<GuidanceCategory>,
    selectedCategoryId: Long?,
    isLoading: Boolean,
    error: String?,
    onCategorySelected: (GuidanceCategory) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = categories,
                key = { it.id },
                contentType = { "category" }
            ) { category ->
                FilterChip(
                    selected = category.id == selectedCategoryId,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.displayName) }
                )
            }
        }
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        if (!error.isNullOrEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ColumnScope.LessonsGridSection(
    uiState: GuidanceUiState,
    onLessonSelected: (GuidanceLesson, Int) -> Unit,
    onRetry: () -> Unit
) {
    when {
        uiState.isLessonsLoading -> {
            // Skeleton list để cải thiện perceived performance thay vì chỉ spinner
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(9, contentType = { "lesson_skeleton" }) { _ ->
                    LessonSkeletonCell()
                }
            }
        }
        uiState.lessonsError != null -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.lessonsError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onRetry) {
                    Text("Thử lại")
                }
            }
        }
        uiState.lessons.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Chưa có bài học cho nhóm cơ này",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Vui lòng chọn nhóm khác hoặc thử lại sau",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = uiState.lessons,
                    key = { lesson -> lesson.id },
                    contentType = { "lesson" }
                ) { lesson ->
                    val index = uiState.lessons.indexOf(lesson)
                    LessonCell(
                        lesson = lesson,
                        number = index + 1,
                        onClick = { onLessonSelected(lesson, index + 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonCell(
    lesson: GuidanceLesson,
    number: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ô vuông chứa số thứ tự
        Box(
            modifier = Modifier
                .size(48.dp)
                .aspectRatio(1f)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Tiêu đề bên phải, có thể kéo ngang nếu quá dài
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun LessonSkeletonCell() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skeleton cho hình ảnh
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Skeleton cho tiêu đề
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun LessonDetailDialog(
    uiState: GuidanceUiState,
    onDismiss: () -> Unit,
    viewModel: GuidanceViewModel,
    isAdmin: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LessonDetailOverlay(
            uiState = uiState,
            onDismiss = onDismiss,
            viewModel = viewModel,
            isAdmin = isAdmin,
            modifier = Modifier
        )
    }
}

@Composable
private fun LessonDetailOverlay(
    uiState: GuidanceUiState,
    onDismiss: () -> Unit,
    viewModel: GuidanceViewModel,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    // ===== OVERDRAW NOTE: Surface with elevation is acceptable for modal overlays =====
    // tonalElevation and shadowElevation create visual separation from background
    // This is intentional UI design for dialogs/overlays, not a performance issue
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        when {
            uiState.isDetailLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.detailError != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.detailError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onDismiss) {
                        Text("Đóng")
                    }
                }
            }
            else -> {
                val detail = uiState.selectedLessonDetail
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bài ${uiState.selectedLessonNumber ?: ""}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.selectedLessonTitle.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            if (isAdmin) {
                                IconButton(onClick = {
                                    uiState.selectedLesson?.let { lesson ->
                                        viewModel.showEditLesson(lesson)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Chỉnh sửa bài học"
                                    )
                                }
                                IconButton(onClick = {
                                    uiState.selectedLesson?.let { lesson ->
                                        viewModel.showConfirmDelete(lesson)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa bài học"
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (detail != null) {
                        LessonDetailContent(detail = detail)
                    } else {
                        Text(
                            text = "Không có dữ liệu bài học",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonDetailContent(detail: GuidanceLessonDetail) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Hiển thị nội dung text trước
        Text(text = detail.content, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Hiển thị hình ảnh nếu có
        detail.imageUrl?.let { imageUrl ->
            var showFullScreenImage by remember { mutableStateOf(false) }

            AsyncImage(
                model = imageUrl,
                contentDescription = "Hình ảnh bài học",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showFullScreenImage = true },
                contentScale = ContentScale.Crop
            )

            if (showFullScreenImage) {
                FullScreenImageDialog(
                    imageUrl = imageUrl,
                    onDismiss = { showFullScreenImage = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hiển thị video nếu có
        detail.videoUrl?.let { videoUrl ->
            VideoPlayer(videoUrl = videoUrl)
        }
    }
}

@UnstableApi
@Composable
private fun VideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerAutoShow = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
        )

        // Full screen button
        IconButton(
            onClick = { isFullScreen = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Toàn màn hình",
                tint = Color.White
            )
        }
    }

    if (isFullScreen) {
        FullScreenVideoDialog(
            videoUrl = videoUrl,
            onDismiss = { isFullScreen = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLessonDialog(
    uiState: GuidanceUiState,
    categories: List<GuidanceCategory>,
    onDismiss: () -> Unit,
    onCreateLesson: (Long, String, String, Uri?, Uri?) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(uiState.selectedCategoryId) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        videoUri = uri
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pendingAction?.invoke()
            pendingAction = null
        }
    }

    fun requestPermissionAndLaunch(launcher: () -> Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            launcher()
        } else {
            val shouldShowRationale = permissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(context as android.app.Activity, permission)
            }
            if (shouldShowRationale) {
                pendingAction = launcher
                permissionLauncher.launch(permissions)
            } else {
                showPermissionDialog = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm bài học mới") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category selection
                Text("Chọn nhóm cơ:", style = MaterialTheme.typography.bodyMedium)
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategoryId = category.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category.displayName)
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Video upload
                Text("Video (tùy chọn):", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { requestPermissionAndLaunch { videoLauncher.launch("video/*") } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (videoUri != null) "Đã chọn video" else "Chọn video")
                }

                // Image upload
                Text("Hình ảnh (tùy chọn):", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { requestPermissionAndLaunch { imageLauncher.launch("image/*") } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imageUri != null) "Đã chọn hình ảnh" else "Chọn hình ảnh")
                }

                if (uiState.createLessonError != null) {
                    Text(
                        text = uiState.createLessonError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedCategoryId?.let { categoryId ->
                        onCreateLesson(categoryId, title, content, videoUri, imageUri)
                    }
                },
                enabled = !uiState.isCreatingLesson && selectedCategoryId != null && title.isNotBlank() && content.isNotBlank()
            ) {
                if (uiState.isCreatingLesson) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Tạo bài học")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Cần cấp quyền") },
            text = { Text("Ứng dụng cần quyền truy cập bộ nhớ để chọn video và hình ảnh. Vui lòng cấp quyền trong cài đặt ứng dụng.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Mở cài đặt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmDeleteDialog(
    lesson: GuidanceLesson?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isDeleting: Boolean
) {
    if (lesson == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác nhận xóa") },
        text = { Text("Bạn có chắc chắn muốn xóa bài học \"${lesson.title}\"? Hành động này không thể hoàn tác.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Xóa")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLessonDialog(
    uiState: GuidanceUiState,
    onDismiss: () -> Unit,
    onUpdateLesson: (Long, String, String, Uri?, Uri?) -> Unit
) {
    val editingLesson = uiState.editingLesson ?: return
    val editingDetail = uiState.editingLessonDetail
    var title by remember { mutableStateOf(editingLesson.title) }
    var content by remember { mutableStateOf(editingDetail?.content ?: "") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        videoUri = uri
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pendingAction?.invoke()
            pendingAction = null
        }
    }

    fun requestPermissionAndLaunch(launcher: () -> Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            launcher()
        } else {
            val shouldShowRationale = permissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(context as android.app.Activity, permission)
            }
            if (shouldShowRationale) {
                pendingAction = launcher
                permissionLauncher.launch(permissions)
            } else {
                showPermissionDialog = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa bài học") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Video upload
                Text("Video (tùy chọn):", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { requestPermissionAndLaunch { videoLauncher.launch("video/*") } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (videoUri != null) "Đã chọn video" else "Chọn video")
                }

                // Image upload
                Text("Hình ảnh (tùy chọn):", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { requestPermissionAndLaunch { imageLauncher.launch("image/*") } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imageUri != null) "Đã chọn hình ảnh" else "Chọn hình ảnh")
                }

                if (uiState.updateLessonError != null) {
                    Text(
                        text = uiState.updateLessonError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateLesson(editingLesson.id, title, content, videoUri, imageUri)
                },
                enabled = !uiState.isUpdatingLesson && title.isNotBlank() && content.isNotBlank()
            ) {
                if (uiState.isUpdatingLesson) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Cập nhật bài học")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Cần cấp quyền") },
            text = { Text("Ứng dụng cần quyền truy cập bộ nhớ để chọn video và hình ảnh. Vui lòng cấp quyền trong cài đặt ứng dụng.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Mở cài đặt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun FullScreenImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Hình ảnh toàn màn hình",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Đóng",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@UnstableApi
@Composable
private fun FullScreenVideoDialog(videoUrl: String, onDismiss: () -> Unit) {
    // Chỗ này có thể áp dụng RenderEffect/AGSL để làm hiệu ứng nền blur hoặc viền glow nếu cần

    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        controllerAutoShow = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Exit full screen button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Thoát toàn màn hình",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

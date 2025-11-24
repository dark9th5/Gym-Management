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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.widget.Toast
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.lc9th5.gym.data.model.GuidanceCategory
import com.lc9th5.gym.data.model.GuidanceLesson
import com.lc9th5.gym.data.model.GuidanceLessonDetail
import com.lc9th5.gym.ui.component.GuidanceVideoPlayer
import com.lc9th5.gym.viewmodel.GuidanceUiState
import com.lc9th5.gym.viewmodel.GuidanceViewModel

@Composable
fun GuidanceScreen(
    modifier: Modifier = Modifier,
    viewModel: GuidanceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "Hướng dẫn tập luyện",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
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

        if (uiState.isDetailVisible) {
            Box(modifier = Modifier.fillMaxSize()) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                )
                LessonDetailOverlay(
                    uiState = uiState,
                    onDismiss = viewModel::closeLessonDetail,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
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
            items(categories, key = { it.id }) { category ->
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(uiState.lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                    LessonCell(
                        number = index + 1,
                        title = lesson.title,
                        onClick = { onLessonSelected(lesson, index + 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonCell(
    number: Int,
    title: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LessonDetailOverlay(
    uiState: GuidanceUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
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
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng"
                            )
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
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(text = detail.content, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (!detail.videoUrl.isNullOrEmpty()) {
            Text(
                text = "Video hướng dẫn",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val rawVideoUrl = detail.videoUrl!!.trim()
            val sanitizedVideoUrl = sanitizeVideoUrl(rawVideoUrl)
            val isYoutubeOrDrive = sanitizedVideoUrl.contains("youtube.com") || sanitizedVideoUrl.contains("youtu.be") || sanitizedVideoUrl.contains("drive.google.com")
            var videoState by remember(sanitizedVideoUrl) { mutableStateOf(if (isYoutubeOrDrive) MediaState.Loading else MediaState.Playing) }
            var youtubeReloadToken by remember(sanitizedVideoUrl) { mutableStateOf(0) }
            val openExternalVideo = remember(rawVideoUrl) {
                {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawVideoUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                    } catch (ignored: ActivityNotFoundException) {
                        Toast.makeText(context, "Không tìm thấy ứng dụng phù hợp", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val reloadKey = "$sanitizedVideoUrl-$youtubeReloadToken"
            val videoBoxModifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)

            Box(modifier = videoBoxModifier) {
                if (isYoutubeOrDrive) {
                    key(reloadKey) {
                        YoutubeWebPlayer(
                            url = sanitizedVideoUrl,
                            onLoaded = { videoState = MediaState.Playing },
                            onError = { videoState = MediaState.Error }
                        )
                    }
                    LaunchedEffect(reloadKey) {
                        videoState = MediaState.Loading
                        delay(12_000)
                        if (videoState == MediaState.Loading) {
                            videoState = MediaState.Error
                        }
                    }
                } else {
                    GuidanceVideoPlayer(
                        videoUrl = sanitizedVideoUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                when (videoState) {
                    MediaState.Loading -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    MediaState.Error -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Không thể phát video",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = {
                                    videoState = MediaState.Loading
                                    youtubeReloadToken++
                                }) {
                                    Text("Thử lại")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(onClick = openExternalVideo) {
                                    Text("Mở YouTube")
                                }
                            }
                        }
                    }

                    MediaState.Playing -> Unit
                }
            }
        }
        if (!detail.imageUrl.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hình ảnh",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            var imageReloadToken by remember(detail.imageUrl) { mutableStateOf(0) }
            val imageRequest = remember(imageReloadToken, detail.imageUrl) {
                ImageRequest.Builder(context)
                    .data(sanitizeImageUrl(detail.imageUrl!!))
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Guidance Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }

                        is AsyncImagePainter.State.Error -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Không tải được hình ảnh",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(onClick = { imageReloadToken++ }) {
                                    Text("Thử lại")
                                }
                            }
                        }

                        else -> SubcomposeAsyncImageContent()
                    }
                }
            }
        }
    }
}

private enum class MediaState {
    Loading,
    Playing,
    Error
}

@Composable
private fun YoutubeWebPlayer(
	url: String,
	onLoaded: () -> Unit,
	onError: () -> Unit
) {
	AndroidView(
		factory = { context ->
			WebView(context).apply {
				setBackgroundColor(AndroidColor.BLACK)
				settings.javaScriptEnabled = true
				settings.domStorageEnabled = true
				settings.mediaPlaybackRequiresUserGesture = false
				settings.useWideViewPort = true
				settings.loadWithOverviewMode = true

				val cookieManager = CookieManager.getInstance()
				cookieManager.setAcceptCookie(true)
				cookieManager.setAcceptThirdPartyCookies(this, true)

				webViewClient = object : WebViewClient() {
					override fun onPageFinished(view: WebView?, url: String?) {
						onLoaded()
					}

					override fun onReceivedError(
						view: WebView?, req: WebResourceRequest?, err: WebResourceError?
					) { onError() }
				}

				val html = """
                    <html>
                    <head>
                        <meta name='viewport' content='width=device-width, initial-scale=1'>
                        <style>
                            html,body { margin:0; padding:0; background:#000; }
                            .wrap { position:relative; padding-top:56.25%; }
                            iframe {
                                position:absolute; top:0; left:0;
                                width:100%; height:100%; border:0;
                            }
                        </style>
                    </head>
                    <body>
                        <div class='wrap'>
                            <iframe 
                                src='$url'
                                referrerpolicy='strict-origin-when-cross-origin'
                                allow='accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; web-share'
                                allowfullscreen
                                playsinline>
                            </iframe>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

				// FIX LỖI 153: PHẢI CÓ baseURL != null
				loadDataWithBaseURL(
					"https://www.youtube.com",
					html,
					"text/html",
					"UTF-8",
					null
				)
			}
		},
		modifier = Modifier.fillMaxSize()
	)
}


private const val GUIDANCE_BASE_URL = "http://192.168.0.108:8080"

private fun sanitizeVideoUrl(raw: String): String {
    var url = raw.trim()

    // Nếu là đường dẫn tương đối từ backend, ghép với base URL
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        val path = if (url.startsWith("/")) url else "/$url"
        url = GUIDANCE_BASE_URL + path
    }

	if (url.contains("youtu.be/")) {
		val id = url.substringAfter("youtu.be/").substringBefore("?")
		url = "https://www.youtube-nocookie.com/embed/$id?rel=0&playsinline=1"
	}
	if (url.contains("youtube.com/watch")) {
		val id = url.substringAfter("v=").substringBefore("&")
		url = "https://www.youtube-nocookie.com/embed/$id?rel=0&playsinline=1"
	}
    if (url.contains("drive.google.com") && url.contains("/view")) {
        val id = url.substringAfter("/d/").substringBefore('/')
        url = "https://drive.google.com/file/d/$id/preview"
    }
    return url
}

private fun sanitizeImageUrl(raw: String): String {
    val trimmed = raw.trim()
    // Nếu là thumbnail YouTube thì chuyển sang proxy nội bộ
    val ytIdFromThumb = if (trimmed.contains("img.youtube.com/vi/")) {
        trimmed.substringAfter("img.youtube.com/vi/").substringBefore('/')
    } else null
    if (ytIdFromThumb != null && ytIdFromThumb.matches(Regex("[a-zA-Z0-9_-]{6,15}"))) {
        return "$GUIDANCE_BASE_URL/proxy/yt-thumb/$ytIdFromThumb"
    }
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    return GUIDANCE_BASE_URL + path
}

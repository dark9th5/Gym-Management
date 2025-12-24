# 🎨 Kỹ Thuật Tối Ưu Giao Diện Đồ Họa - Android Jetpack Compose

**Ngày tạo:** 18/12/2024  
**Áp dụng cho:** Gym Android App (Jetpack Compose)

---

## 📖 Mục Lục

1. [Tổng quan về Rendering trong Compose](#1-tổng-quan-về-rendering-trong-compose)
2. [Kỹ thuật Remember & State](#2-kỹ-thuật-remember--state)
3. [Kỹ thuật Lazy Layouts](#3-kỹ-thuật-lazy-layouts)
4. [Kỹ thuật Derivation & Stability](#4-kỹ-thuật-derivation--stability)
5. [Kỹ thuật tối ưu Images](#5-kỹ-thuật-tối-ưu-images)
6. [Kỹ thuật tối ưu Animations](#6-kỹ-thuật-tối-ưu-animations)
7. [Kỹ thuật Layout Optimization](#7-kỹ-thuật-layout-optimization)
8. [Kỹ thuật Side Effects](#8-kỹ-thuật-side-effects)
9. [Profiling & Debugging Tools](#9-profiling--debugging-tools)
10. [Áp dụng vào Gym App](#10-áp-dụng-vào-gym-app)

---

## 1. Tổng quan về Rendering trong Compose

### 3 Giai đoạn Rendering:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Composition │ -> │   Layout    │ -> │   Drawing   │
│  (Cái gì?)  │    │ (Ở đâu?)    │    │ (Vẽ thế nào?)│
└─────────────┘    └─────────────┘    └─────────────┘
```

| Giai đoạn | Mô tả | Khi nào chạy lại |
|-----------|-------|------------------|
| **Composition** | Xác định UI tree, gọi composable functions | State thay đổi |
| **Layout** | Đo và đặt vị trí các elements | Size/position thay đổi |
| **Drawing** | Vẽ pixels lên canvas | Visual properties thay đổi |

### Nguyên tắc tối ưu:
> **Mục tiêu:** Skip càng nhiều giai đoạn càng tốt khi có thay đổi

---

## 2. Kỹ Thuật Remember & State

### 2.1. remember {} - Tránh recomputation

#### ❌ Không tối ưu:
```kotlin
@Composable
fun ExerciseList(exercises: List<Exercise>) {
    // ⚠️ sortedExercises được tính lại MỖI LẦN recomposition
    val sortedExercises = exercises.sortedBy { it.name }
    
    LazyColumn {
        items(sortedExercises) { exercise ->
            ExerciseItem(exercise)
        }
    }
}
```

#### ✅ Tối ưu với remember:
```kotlin
@Composable
fun ExerciseList(exercises: List<Exercise>) {
    // ✅ Chỉ tính lại khi exercises thay đổi
    val sortedExercises = remember(exercises) {
        exercises.sortedBy { it.name }
    }
    
    LazyColumn {
        items(sortedExercises) { exercise ->
            ExerciseItem(exercise)
        }
    }
}
```

### 2.2. derivedStateOf - Cho computed values

```kotlin
@Composable
fun WorkoutProgress(completedSets: Int, totalSets: Int) {
    // ✅ Chỉ recompose khi KẾT QUẢ progressPercent thay đổi
    val progressPercent by remember {
        derivedStateOf { 
            if (totalSets > 0) (completedSets * 100) / totalSets else 0 
        }
    }
    
    Text("Tiến độ: $progressPercent%")
}
```

### 2.3. rememberSaveable - Giữ state qua configuration change

```kotlin
@Composable
fun WorkoutScreen() {
    // ✅ Giữ nguyên selectedTab khi xoay màn hình
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    
    TabRow(selectedTabIndex = selectedTab) { ... }
}
```

---

## 3. Kỹ Thuật Lazy Layouts

### 3.1. LazyColumn/LazyRow - Virtualization

#### ❌ Không tối ưu:
```kotlin
@Composable
fun ExerciseList(exercises: List<Exercise>) {
    // ⚠️ Tất cả items được compose cùng lúc, dù không nhìn thấy
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        exercises.forEach { exercise ->
            ExerciseItem(exercise)
        }
    }
}
```

#### ✅ Tối ưu với LazyColumn:
```kotlin
@Composable
fun ExerciseList(exercises: List<Exercise>) {
    // ✅ Chỉ compose items đang hiển thị trên màn hình
    LazyColumn {
        items(
            items = exercises,
            key = { it.id }  // ✅ QUAN TRỌNG: Cung cấp unique key
        ) { exercise ->
            ExerciseItem(exercise)
        }
    }
}
```

### 3.2. Key - Tránh recomposition không cần thiết

```kotlin
LazyColumn {
    items(
        items = workoutPlans,
        key = { plan -> plan.id }  // ✅ Key ổn định giúp Compose track items
    ) { plan ->
        PlanCard(plan)
    }
}
```

**Tại sao key quan trọng?**
- Không có key: Compose không biết item nào đã thay đổi → recompose tất cả
- Có key: Compose chỉ recompose item thực sự thay đổi

### 3.3. contentType - Tối ưu cho mixed content

```kotlin
LazyColumn {
    items(
        items = workoutItems,
        key = { it.id },
        contentType = { item ->
            when (item) {
                is Exercise -> "exercise"
                is RestPeriod -> "rest"
                else -> "other"
            }
        }
    ) { item ->
        when (item) {
            is Exercise -> ExerciseItem(item)
            is RestPeriod -> RestItem(item)
        }
    }
}
```

---

## 4. Kỹ Thuật Derivation & Stability

### 4.1. Stable Classes - Giúp Compose skip recomposition

#### ❌ Unstable class (luôn recompose):
```kotlin
// ⚠️ List là unstable type trong Kotlin
data class WorkoutState(
    val exercises: List<Exercise>,  // Unstable!
    val isLoading: Boolean
)
```

#### ✅ Stable class:
```kotlin
import androidx.compose.runtime.Immutable

@Immutable  // ✅ Đánh dấu class này là immutable
data class WorkoutState(
    val exercises: List<Exercise>,
    val isLoading: Boolean
)

// Hoặc sử dụng @Stable cho class có thể thay đổi nhưng Compose có thể track
@Stable
class MutableWorkoutState {
    var exercises by mutableStateOf(emptyList<Exercise>())
    var isLoading by mutableStateOf(false)
}
```

### 4.2. Lambda Stability - Tránh recomposition từ lambdas

#### ❌ Unstable lambda:
```kotlin
@Composable
fun ExerciseItem(exercise: Exercise) {
    Button(
        // ⚠️ Lambda mới được tạo mỗi lần recomposition
        onClick = { viewModel.completeExercise(exercise.id) }
    ) {
        Text("Hoàn thành")
    }
}
```

#### ✅ Stable lambda với remember:
```kotlin
@Composable
fun ExerciseItem(
    exercise: Exercise,
    onComplete: (String) -> Unit  // ✅ Nhận lambda từ parent
) {
    val onClick = remember(exercise.id) {
        { onComplete(exercise.id) }
    }
    
    Button(onClick = onClick) {
        Text("Hoàn thành")
    }
}
```

---

## 5. Kỹ Thuật Tối Ưu Images

### 5.1. Coil - Async Image Loading

```kotlin
// ✅ Sử dụng Coil cho image loading hiệu quả
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(exercise.imageUrl)
        .crossfade(true)
        .memoryCachePolicy(CachePolicy.ENABLED)  // ✅ Memory cache
        .diskCachePolicy(CachePolicy.ENABLED)    // ✅ Disk cache
        .build(),
    contentDescription = exercise.name,
    modifier = Modifier
        .size(100.dp)
        .clip(RoundedCornerShape(8.dp)),
    contentScale = ContentScale.Crop
)
```

### 5.2. Placeholder & Error States

```kotlin
AsyncImage(
    model = imageUrl,
    contentDescription = null,
    placeholder = painterResource(R.drawable.placeholder),  // ✅ Loading state
    error = painterResource(R.drawable.error_image),        // ✅ Error state
    modifier = Modifier.fillMaxWidth()
)
```

### 5.3. Image Sizing - Tránh decode full resolution

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .size(Size(200, 200))  // ✅ Chỉ decode size cần thiết
        .scale(Scale.FILL)
        .build(),
    contentDescription = null
)
```

---

## 6. Kỹ Thuật Tối Ưu Animations

### 6.1. animateContentSize - Smooth size changes

```kotlin
Column(
    modifier = Modifier
        .animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
) {
    // Content that changes size
}
```

### 6.2. Animate*AsState - Cho single values

```kotlin
// ✅ Animation không trigger recomposition liên tục
val progress by animateFloatAsState(
    targetValue = if (isCompleted) 1f else 0f,
    animationSpec = tween(durationMillis = 300),
    label = "progress"
)

LinearProgressIndicator(progress = progress)
```

### 6.3. updateTransition - Cho multiple values

```kotlin
val transition = updateTransition(targetState = isExpanded, label = "expand")

val height by transition.animateDp(label = "height") { expanded ->
    if (expanded) 200.dp else 56.dp
}

val alpha by transition.animateFloat(label = "alpha") { expanded ->
    if (expanded) 1f else 0.5f
}
```

### 6.4. Modifier.graphicsLayer - GPU-accelerated

```kotlin
// ✅ Animations trong graphicsLayer không trigger recomposition
Box(
    modifier = Modifier.graphicsLayer {
        alpha = animatedAlpha
        scaleX = animatedScale
        scaleY = animatedScale
        rotationZ = animatedRotation
    }
)
```

---

## 7. Kỹ Thuật Layout Optimization

### 7.1. Modifier Order Matters

#### ❌ Sai thứ tự:
```kotlin
Box(
    modifier = Modifier
        .padding(16.dp)      // Padding trước
        .background(Color.Red)  // Background sau → không bao gồm padding area
)
```

#### ✅ Đúng thứ tự:
```kotlin
Box(
    modifier = Modifier
        .background(Color.Red)  // Background trước
        .padding(16.dp)         // Padding sau → background bao gồm padding
)
```

### 7.2. Intrinsic Measurements

```kotlin
// ✅ Đo intrinsic size để tránh multiple measurement passes
Row(modifier = Modifier.height(IntrinsicSize.Min)) {
    Text("Short")
    Divider(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
    )
    Text("This is a much longer text")
}
```

### 7.3. SubcomposeLayout - Lazy composition

```kotlin
// ✅ Compose only when needed (used in LazyColumn internally)
SubcomposeLayout { constraints ->
    val mainPlaceable = subcompose("main") {
        MainContent()
    }.first().measure(constraints)
    
    layout(mainPlaceable.width, mainPlaceable.height) {
        mainPlaceable.place(0, 0)
    }
}
```

### 7.4. BoxWithConstraints - Responsive layouts

```kotlin
// ✅ Chỉ compose content phù hợp với screen size
BoxWithConstraints {
    if (maxWidth < 600.dp) {
        PhoneLayout()
    } else {
        TabletLayout()
    }
}
```

---

## 8. Kỹ Thuật Side Effects

### 8.1. LaunchedEffect - One-time operations

```kotlin
@Composable
fun WorkoutScreen(workoutId: String) {
    // ✅ Chỉ chạy khi workoutId thay đổi
    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }
}
```

### 8.2. DisposableEffect - Cleanup resources

```kotlin
@Composable
fun VideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    
    DisposableEffect(videoUrl) {
        val player = ExoPlayer.Builder(context).build()
        player.setMediaItem(MediaItem.fromUri(videoUrl))
        player.prepare()
        
        onDispose {
            player.release()  // ✅ Cleanup khi composable leaves composition
        }
    }
}
```

### 8.3. SideEffect - Sync với non-Compose code

```kotlin
@Composable
fun AnalyticsScreen(screenName: String) {
    // ✅ Chạy sau mỗi successful recomposition
    SideEffect {
        analytics.logScreenView(screenName)
    }
}
```

### 8.4. produceState - Convert non-Compose state

```kotlin
@Composable
fun NetworkStatus(): State<Boolean> {
    return produceState(initialValue = false) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = true }
            override fun onLost(network: Network) { value = false }
        }
        connectivityManager.registerNetworkCallback(networkRequest, callback)
        
        awaitDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
```

---

## 9. Profiling & Debugging Tools

### 9.1. Android Studio Layout Inspector

1. **Run** app trên device/emulator
2. **View → Tool Windows → Layout Inspector**
3. Xem component tree và properties

### 9.2. Compose Compiler Metrics

Thêm vào `build.gradle.kts`:
```kotlin
kotlinOptions {
    freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
            project.buildDir.absolutePath + "/compose_metrics"
    )
    freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
            project.buildDir.absolutePath + "/compose_reports"
    )
}
```

### 9.3. Recomposition Highlighter

```kotlin
// Debug: Highlight recompositions
@Composable
fun RecompositionCounter(label: String) {
    val count = remember { mutableIntStateOf(0) }
    SideEffect { count.intValue++ }
    
    if (BuildConfig.DEBUG) {
        Text("$label: ${count.intValue} recompositions", color = Color.Red)
    }
}
```

### 9.4. GPU Profiling

1. **Developer Options → Profile GPU Rendering → On screen as bars**
2. Theo dõi các thanh màu:
   - 🟢 **Green line** = 16ms (60 FPS target)
   - Bars vượt qua green line = frame drops

---

## 10. Áp Dụng Vào Gym App

### 10.1. Các file cần kiểm tra trong Gym App:

| File | Kỹ thuật cần áp dụng |
|------|----------------------|
| `WorkoutScreen.kt` | LazyColumn với keys, remember cho computed values |
| `StatisticsScreen.kt` | derivedStateOf cho chart calculations |
| `GuidanceScreen.kt` | AsyncImage với caching, DisposableEffect cho video |
| `WorkoutPlanScreen.kt` | Stable data classes, lambda optimization |

### 10.2. Ví dụ cụ thể cho Gym App:

#### WorkoutScreen - Exercise List:
```kotlin
// Hiện tại có thể dùng:
LazyColumn {
    items(exercises) { exercise -> ... }
}

// Nên sửa thành:
LazyColumn {
    items(
        items = exercises,
        key = { it.id }  // ✅ Thêm key
    ) { exercise ->
        ExerciseItem(
            exercise = exercise,
            onComplete = remember(exercise.id) {  // ✅ Stable lambda
                { viewModel.completeExercise(exercise.id) }
            }
        )
    }
}
```

#### StatisticsScreen - Chart Data:
```kotlin
// Sử dụng derivedStateOf cho filtered data
val filteredStats by remember(exerciseId, dateRange) {
    derivedStateOf {
        allStats.filter { 
            it.exerciseId == exerciseId && 
            it.date in dateRange 
        }
    }
}
```

#### GuidanceScreen - Video Player:
```kotlin
@Composable
fun VideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    
    // ✅ Proper lifecycle management
    DisposableEffect(videoUrl) {
        val player = ExoPlayer.Builder(context).build()
        // setup player...
        
        onDispose {
            player.release()  // ✅ Cleanup
        }
    }
}
```

---

## 📋 Checklist Tối Ưu UI

### Composition Phase:
- [ ] Sử dụng `remember` cho expensive calculations
- [ ] Sử dụng `derivedStateOf` cho computed values
- [ ] Cung cấp `key` cho LazyColumn/LazyRow items
- [ ] Đánh dấu data classes với `@Immutable` hoặc `@Stable`
- [ ] Tránh creating lambdas trong composable body

### Layout Phase:
- [ ] Sử dụng `LazyColumn/LazyRow` thay vì `Column/Row` với scroll
- [ ] Đúng thứ tự modifiers
- [ ] Sử dụng `BoxWithConstraints` cho responsive layouts

### Drawing Phase:
- [ ] Giảm GPU Overdraw (xem tài liệu riêng)
- [ ] Sử dụng `Modifier.graphicsLayer` cho animations
- [ ] Clip images đúng size cần thiết

### Images:
- [ ] Sử dụng `AsyncImage` với caching
- [ ] Cung cấp placeholders và error states
- [ ] Resize images phù hợp

### Side Effects:
- [ ] Sử dụng đúng loại effect (LaunchedEffect, DisposableEffect, etc.)
- [ ] Cleanup resources trong onDispose

---

## 📚 Tài Liệu Tham Khảo

- [Jetpack Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [Compose Phases](https://developer.android.com/jetpack/compose/phases)
- [State and Jetpack Compose](https://developer.android.com/jetpack/compose/state)
- [Side-effects in Compose](https://developer.android.com/jetpack/compose/side-effects)
- [Thinking in Compose](https://developer.android.com/jetpack/compose/mental-model)

---

*Tài liệu này cung cấp hướng dẫn tổng quan về các kỹ thuật tối ưu giao diện trong Jetpack Compose.*

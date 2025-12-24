# BÁO CÁO TỐI ƯU HÓA GIAO DIỆN VÀ ĐỒ HỌA DỰ ÁN GYM APP

## Chương 2: Các kỹ thuật tối ưu giao diện, đồ họa

Trong phát triển ứng dụng di động hiện đại, đặc biệt là với Jetpack Compose, việc tối ưu hóa giao diện (UI) và đồ họa không chỉ dừng lại ở việc làm đẹp mà còn ảnh hưởng trực tiếp đến hiệu năng (Performance), độ mượt mà (FPS) và mức tiêu thụ pin của thiết bị. Dưới đây là các kỹ thuật cốt lõi đã được nghiên cứu và áp dụng vào dự án.

### 2.1. Kỹ thuật giảm thiểu Overdraw (Vẽ chồng)
**Overdraw** là hiện tượng GPU phải vẽ lại nhiều lần trên cùng một pixel trong một khung hình (frame). Đây là nguyên nhân hàng đầu gây ra sự lãng phí tài nguyên xử lý đồ họa.

*   **Nguyên lý:** Mỗi lớp nền (background) hoặc thành phần UI chồng lên nhau đều yêu cầu GPU thực hiện lệnh vẽ. Nếu một pixel bị vẽ đè 4-5 lần (Overdraw 4x+), GPU sẽ phải làm việc gấp nhiều lần mức cần thiết.
*   **Giải pháp áp dụng:**
    *   **Loại bỏ Background mặc định:** Xóa bỏ màu nền mặc định của `windowBackground` ở cấp độ Theme hoặc Activity nếu giao diện Compose đã tự vẽ nền của nó.
        
        **Ví dụ code:**
        *Trước khi tối ưu (themes.xml):*
        ```xml
        <style name="Theme.Gym" parent="android:Theme.Material.Light.NoActionBar">
            <item name="android:windowBackground">@color/white</item>
        </style>
        ```
        *Sau khi tối ưu:*
        ```xml
        <style name="Theme.Gym" parent="android:Theme.Material.Light.NoActionBar">
            <!-- Đặt null để GPU không cần vẽ lớp nền window -->
            <item name="android:windowBackground">@null</item> 
        </style>
        ```

    *   **Flatten Layout (Làm phẳng cấu trúc):** Hạn chế tối đa việc lồng ghép các container như `Box`, `Column`, `Row` không cần thiết.
        
        **Ví dụ code:**
        *Trước khi tối ưu (Lồng ghép thừa):*
        ```kotlin
        Box(modifier = Modifier.fillMaxWidth()) {
            // Dùng Box chỉ để căn giữa Button -> thừa 1 layer
            Button(
                modifier = Modifier.align(Alignment.Center),
                onClick = { }
            ) { Text("Click") }
        }
        ```
        *Sau khi tối ưu:*
        ```kotlin
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally // Xử lý alignment ngay tại cha
        ) {
            Button(onClick = { }) { Text("Click") }
        }
        ```

    *   **Transparency Management (Quản lý độ trong suốt):** Tránh sử dụng `Surface` hoặc `Card` với màu `Color.Transparent` chỉ để bao bọc (wrapper).
        
        **Ví dụ code (HomeScreen.kt):**
        *Trước khi tối ưu:*
        ```kotlin
        Surface(
            color = Color.Transparent, // Vẫn tốn chi phí vẽ layer trong suốt
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.background(GradientBackground)) { /* Content */ }
        }
        ```
        *Sau khi tối ưu:*
        ```kotlin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientBackground) // Vẽ trực tiếp background, bỏ Surface wrapper
        ) { /* Content */ }
        ```

    *   **Tối ưu hóa Scaffold:** Sử dụng triệt để các slot có sẵn của `Scaffold`.
        
        **Ví dụ code (WorkoutScreen.kt):**
        *Trước khi tối ưu:*
        ```kotlin
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = MaterialTheme.colorScheme.background) { ... }
            // Tự chồng FAB lên trên, gây overdraw vùng FAB đè lên Scaffold
            FloatingActionButton(
                modifier = Modifier.align(Alignment.BottomEnd) 
            ) { ... }
        }
        ```
        *Sau khi tối ưu:*
        ```kotlin
        Scaffold(
            // Dùng slot chuẩn, hệ thống tự tối ưu việc vẽ
            floatingActionButton = { FloatingActionButton(...) },
            // Tránh vẽ lại background màu trắng nếu màn hình cha (HomeScreen) đã vẽ rồi
            containerColor = Color.Transparent 
        ) { ... }
        ```

### 2.2. Tối ưu hóa Recomposition (Vẽ lại) thông minh
Trong Jetpack Compose, Recomposition là quá trình chạy lại mã UI để cập nhật giao diện khi dữ liệu thay đổi. Recomposition không cần thiết là kẻ thù của hiệu năng.

*   **Sử dụng Smart Keys:** Cung cấp `key` ổn định cho `LazyColumn`.
    
    **Ví dụ code (WorkoutScreen.kt):**
    *Trước khi tối ưu:*
    ```kotlin
    items(items = uiState.pendingExercises) { exercise -> 
        // Khi list thay đổi, Compose có thể phải vẽ lại toàn bộ vì không biết item nào là item nào
        PendingExerciseItem(exercise)
    }
    ```
    *Sau khi tối ưu:*
    ```kotlin
    items(
        items = uiState.pendingExercises,
        key = { exercise -> "pending_${exercise.id}" } // Unique Key giúp định danh
    ) { exercise ->
        PendingExerciseItem(exercise)
    }
    ```

*   **Data Class Stability:** Sử dụng annotation `@Immutable`.
    
    **Ví dụ code (WorkoutModels.kt):**
    *Trước khi tối ưu:*
    ```kotlin
    data class WorkoutPlan(
        val id: Long,
        val days: List<WorkoutPlanDay> // List là interface, Compose coi là unstable
    )
    ```
    *Sau khi tối ưu:*
    ```kotlin
    @androidx.compose.runtime.Immutable // Cam kết dữ liệu không thay đổi ngầm
    data class WorkoutPlan(
        val id: Long,
        val days: List<WorkoutPlanDay>
    )
    ```

### 2.3. Quản lý tài nguyên và hiển thị
*   **Vector Graphics:** Ưu tiên sử dụng Vector Drawable.
    
    **Ví dụ code:**
    *Trước khi tối ưu:*
    ```kotlin
    Image(
        painter = painterResource(id = R.drawable.ic_add_png), // Bitmap tốn bộ nhớ, vỡ khi zoom
        contentDescription = null
    )
    ```
    *Sau khi tối ưu:*
    ```kotlin
    Icon(
        imageVector = Icons.Default.Add, // Vector path nhẹ, sắc nét mọi độ phân giải
        contentDescription = null
    )
    ```

*   **Lazy Loading:** Chỉ tải ảnh khi cần thiết.
    
    **Ví dụ code (GuidanceScreen.kt):**
    *Trước khi tối ưu:*
    ```kotlin
    Image(
        // Tải đồng bộ hoặc không cache hiệu quả
        bitmap = loadImageBitmap(url), 
        contentDescription = null
    )
    ```
    *Sau khi tối ưu:*
    ```kotlin
    AsyncImage(
        model = imageUrl, // Tải bất đồng bộ, tự động cache, tự hủy khi scroll khuất
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
    ```

### 2.4. Tối ưu hóa hiệu năng cảm nhận (Perceived Performance)
Hiệu năng cảm nhận là tốc độ mà người dùng *cảm thấy* ứng dụng phản hồi, đôi khi còn quan trọng hơn tốc độ xử lý thực tế của máy.

*   **Skeleton Loading (Màn hình xương):** Thay vì hiển thị màn hình trắng hoặc vòng quay vô tận (Spinner) khi đang tải dữ liệu, ứng dụng hiển thị khung cấu trúc (Skeleton) của nội dung. Điều này tạo cảm giác dữ liệu sắp xuất hiện và giảm sự khó chịu.
    
    **Ví dụ code (GuidanceScreen.kt):**
    *Trước khi tối ưu:*
    ```kotlin
    if (isLoading) {
        // Chỉ hiện spinner đơn điệu, người dùng không biết nội dung gì sắp hiện
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) 
    }
    ```
    *Sau khi tối ưu:*
    ```kotlin
    if (isLoading) {
        // Hiện khung giả lập cấu trúc bài học (LessonSkeletonCell)
        LazyColumn {
            items(5) { LessonSkeletonCell() } 
        }
    }
    // LessonSkeletonCell sử dụng Box màu xám nhạt với hiệu ứng pulse để giả lập
    ```



*   **Smooth Transitions (Hiệu ứng chuyển đổi mượt mà):** Sử dụng `AnimatedContent` hoặc `Crossfade` thay vì thay đổi nội dung đột ngột (`if/else`), giúp não bộ người dùng dễ dàng theo dõi sự thay đổi ngữ cảnh.
    
    **Ví dụ code (HomeScreen.kt):**
    *Trước khi tối ưu:*
    ```kotlin
    // Chuyển tab giật cục
    if (selectedTab == 0) ScreenA() else ScreenB()
    ```
    *Sau khi tối ưu:*
    ```kotlin
    // Chuyển tab mượt mà với hiệu ứng Fade In/Out
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { tab ->
        if (tab == 0) ScreenA() else ScreenB()
    }
    ```

---

## Chương 3: Các công cụ kiểm tra giao diện, đồ họa

Để đảm bảo hiệu quả của quá trình tối ưu, chúng ta cần các công cụ đo đạc chính xác. Dự án sử dụng bộ công cụ tiêu chuẩn của Android Studio và các tùy chọn Developer Options trên thiết bị thật.

### 3.1. Debug GPU Overdraw
Đây là công cụ trực quan nhất để phát hiện các vấn đề về vẽ chồng (Overdraw).
*   **Cách kích hoạt:** Cài đặt thiết bị (Settings) -> Tùy chọn nhà phát triển (Developer Options) -> Hardware accelerated rendering -> Debug GPU overdraw -> Chọn "Show overdraw areas".
*   **Hệ thống màu sắc cảnh báo:**
    *   **Không màu / Màu gốc:** Vẽ 1 lần (Best) - Không có overdraw.
    *   **Xanh dương:** Vẽ 2 lần (Good) - Mức độ lý tưởng cho các UI có background và nội dung text/icon bên trên.
    *   **Xanh lá:** Vẽ 3 lần (Acceptable) - Chấp nhận được ở các khu vực UI phức tạp, có hiệu ứng đổ bóng hoặc thẻ nổi.
    *   **Đỏ nhạt:** Vẽ 4 lần (Warning) - Cần xem xét tối ưu.
    *   **Đỏ đậm:** Vẽ 5 lần trở lên (Bad) - Lãng phí tài nguyên nghiêm trọng, cần khắc phục ngay.

### 3.2. Layout Inspector
Công cụ tích hợp trong Android Studio cho phép "soi" cấu trúc cây giao diện (View Hierarchy/Component Tree) đang chạy thời gian thực.
*   **Công dụng:** Giúp phát hiện các Composable bị lồng nhau quá sâu (Deep nesting) mà mắt thường không thấy được. Nó cũng hiển thị các thuộc tính layout, kích thước và recomposition count (số lần vẽ lại) của từng thành phần.

### 3.3. Profile GPU Rendering (HWUI Rendering)
Công cụ đo lường thời gian cần thiết để CPU và GPU hoàn thành việc vẽ một khung hình.
*   **Hiển thị:** Dạng biểu đồ thanh trượt trên màn hình.
*   **Tiêu chuẩn:** Một đường kẻ ngang màu xanh lá cây đại diện cho ngưỡng **16ms** (tương đương 60 FPS).
*   **Phân tích:** Nếu các thanh biểu đồ vượt quá đường xanh lá, nghĩa là khung hình đó mất hơn 16ms để vẽ, gây ra hiện tượng giật (jank/dropped frame).

---

## Chương 4: Triển khai và thực nghiệm

Dựa trên cơ sở lý thuyết (Chương 2) và các công cụ phân tích (Chương 3), đội ngũ phát triển đã tiến hành một đợt Refactor (tái cấu trúc) mã nguồn quy mô lớn nhằm giải quyết triệt để các vấn đề hiệu năng.

### 4.1. Phân tích hiện trạng (Trước khi tối ưu)
Sử dụng công cụ **Debug GPU Overdraw**, chúng tôi đã xác định được các "điểm nóng" (hotspots) hiệu năng sau:

1.  **Overdraw nghiêm trọng:**
    *   **WorkoutScreen:** Màn hình tập luyện bị phủ một lớp màu Đỏ/Đỏ sẫm. Nguyên nhân do việc lồng ghép 3 lớp background chồng lên nhau: `Scaffold` (trắng) -> `Box` wrapper (trắng) -> `Card` bài tập (Card background).
    *   **TopBar:** Khu vực thanh tiêu đề có màu Đỏ do sử dụng `Surface` (trong suốt) bao ngoài `Box` (gradient). Dù trong suốt, `Surface` vẫn tạo ra một pass vẽ.
2.  **Jank (Giật lag) khi cuộn:**
    *   Danh sách bài tập (`LazyColumn`) thỉnh thoảng bị khựng lại khi thêm/xóa phần tử. Nguyên nhân do thiếu tham số `key`, khiến Compose phải tính toán lại vị trí của tất cả các item thay vì chỉ item bị thay đổi.

### 4.2. Chi tiết triển khai (Implementation Details)

#### A. Giảm thiểu Overdraw & Làm phẳng Layout

**1. Theme (themes.xml):**
Loại bỏ lớp nền mặc định của cửa sổ ứng dụng.
*   *Trước:* `<item name="android:windowBackground">@color/white</item>`
*   *Sau:* `<item name="android:windowBackground">@null</item>`

**2. HomeScreen (TopBar):**
Loại bỏ wrapper `Surface` không cần thiết.

*   *Code thực tế:*
    ```kotlin
    // Trước: Surface thừa tạo thêm 1 lớp vẽ
    // Surface(color = Color.Transparent) { 
    //    Box(modifier = Modifier.background(brush = ...)) { ... }
    // }

    // Sau: Vẽ background trực tiếp lên Box container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(colors = listOf(PrimaryLight, PrimaryLight.copy(alpha = 0.9f)))
            )
    ) { ... }
    ```

**3. WorkoutScreen (Scaffold):**
Sử dụng đúng slot `floatingActionButton` của `Scaffold` thay vì lồng `Box`.

*   *Code thực tế:*
    ```kotlin
    // Trước: Lồng Box để đặt FAB -> Overdraw
    // Box {
    //     Scaffold(containerColor = Color.White) { ... }
    //     FloatingActionButton(...)
    // }

    // Sau: Tận dụng cấu trúc Scaffold
    Scaffold(
        containerColor = Color.Transparent, // Trong suốt để tận dụng background cha
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExerciseDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Thêm")
            }
        }
    ) { padding -> ... }
    ```

#### B. Tối ưu hóa Recomposition (Dữ liệu ổn định)

**1. Smart Keys (LazyColumn - WorkoutScreen):**
Cung cấp định danh duy nhất cho từng mục trong danh sách cuộn.

*   *Code thực tế:*
    ```kotlin
    items(
        items = uiState.pendingExercises,
        key = { exercise -> "pending_${exercise.id}" } // Key ổn định giúp Compose theo dõi item
    ) { exercise ->
        PendingExerciseItem(
            exercise = exercise,
            onComplete = { viewModel.completePendingExercise(exercise) }
        )
    }
    ```

**2. Immutable Data (WorkoutModel.kt):**

**Code thực tế (`WorkoutModel.kt`):**
```kotlin
@androidx.compose.runtime.Immutable
data class WorkoutPlan(
    val id: Long,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val days: List<WorkoutPlanDay> // List thường được coi là unstable, nhưng @Immutable sẽ 'ép' nó thành stable
)
```

#### C. Nâng cao hiệu năng cảm nhận (Perceived Performance)

**1. Skeleton Loading (GuidanceScreen):**
Thay thế spinner đơn điệu bằng hiệu ứng khung xương tải dữ liệu.

**Code thực tế:**
```kotlin
// Tạo hiệu ứng nhấp nháy (Pulse animation)
val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
val alpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000), 
        repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
)

// Áp dụng vào Box giả lập nội dung
Box(
    modifier = Modifier
        .size(64.dp)
        .background(Color.Gray.copy(alpha = alpha)) // Hiệu ứng pulse tại đây
)
```

**2. Smooth Transitions (HomeScreen):**
Loại bỏ việc chuyển màn hình kiểu "cắt cảnh" đột ngột, sử dụng `AnimatedContent` để tạo hiệu ứng mượt mà.

**Code thực tế:**
```kotlin
AnimatedContent(
    targetState = selectedTab,
    transitionSpec = {
        // Fade in màn hình mới, Fade out màn hình cũ trong 300ms
        fadeIn(animationSpec = tween(300)) togetherWith 
        fadeOut(animationSpec = tween(300))
    },
    label = "tab_transition"
) { tab ->
    when (tab) {
        0 -> HomeScreen(...)
        1 -> WorkoutLogScreen(...)
        2 -> StatisticsScreen(...)
    }
}
```

### 4.3. Kết quả thực nghiệm

Sau khi áp dụng các giải pháp trên, chúng tôi đã tiến hành kiểm tra lại trên thiết bị thật và ghi nhận những cải thiện rõ rệt về mặt thị giác và cảm giác sử dụng:

1.  **Về Overdraw:**
    *   Các vùng màu Đỏ đậm trên `WorkoutScreen` đã hoàn toàn biến mất.
    *   Giao diện chủ yếu hiển thị màu Xanh dương (1x overdraw) và Xanh lá (2x overdraw), đây là mức độ tối ưu lý tưởng cho ứng dụng có background phức tạp.

2.  **Về độ mượt mà (Smoothness):**
    *   Thao tác cuộn danh sách bài tập trở nên "dính tay" và mượt hơn, không còn hiện tượng khựng nhẹ khi danh sách dài.
    *   Việc chuyển đổi giữa các tab chức năng diễn ra nhẹ nhàng, tự nhiên nhờ hiệu ứng Fade, tạo cảm giác ứng dụng được đầu tư kỹ lưỡng (Polished).

3.  **Về tài nguyên hệ thống:**
    *   Biểu đồ GPU Profiling cho thấy các thanh cột hiếm khi vượt quá ngưỡng an toàn, chứng tỏ GPU không còn bị quá tải.
    *   Nhiệt độ thiết bị ổn định hơn khi sử dụng ứng dụng trong thời gian dài do CPU/GPU không phải xử lý các tác vụ vẽ thừa thãi.

**Kết luận:**
Việc loại bỏ Overdraw và áp dụng các hiệu ứng chuyển cảnh hợp lý đã giúp Gym App "lột xác" từ một ứng dụng có phần nặng nề thành một sản phẩm mượt mà, mang lại trải nghiệm người dùng cao cấp (Premium UX).

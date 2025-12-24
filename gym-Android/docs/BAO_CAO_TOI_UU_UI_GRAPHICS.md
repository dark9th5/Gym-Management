# 📑 BÁO CÁO TỐI ƯU HÓA GIAO DIỆN VÀ ĐỒ HỌA - GYM APP ANDROID

**Dự án:** Gym Management App  
**Công nghệ:** Android Jetpack Compose  
**Ngày lập:** 18/12/2024

---

## 📖 MỤC LỤC

1. [Chương 1: Tổng quan về tối ưu giao diện và đồ họa](#chương-1-tổng-quan-về-tối-ưu-giao-diện-và-đồ-họa)
2. [Chương 2: Các kỹ thuật tối ưu giao diện, đồ họa](#chương-2-các-kỹ-thuật-tối-ưu-giao-diện-đồ-họa)
3. [Chương 3: Các công cụ kiểm tra giao diện và đồ họa](#chương-3-các-công-cụ-kiểm-tra-giao-diện-và-đồ-họa)
4. [Chương 4: Triển khai và thực nghiệm](#chương-4-triển-khai-và-thực-nghiệm)

---

## CHƯƠNG 1: TỔNG QUAN VỀ TỐI ƯU GIAO DIỆN VÀ ĐỒ HỌA

### 1.1. Tại sao cần tối ưu hóa?
Trong phát triển ứng dụng di động, đặc biệt là Android, hiệu năng giao diện (UI Performance) đóng vai trò then chốt trong trải nghiệm người dùng (UX). Một ứng dụng được tối ưu tốt sẽ mang lại:
- **Độ mượt mà (Smoothness):** Đạt chuẩn 60 FPS (hoặc 90/120 FPS trên thiết bị mới), mỗi khung hình được vẽ trong dưới 16ms.
- **Tiết kiệm pin:** Giảm tải cho GPU và CPU giúp thiết bị hoạt động lâu hơn.
- **Giảm nhiệt độ:** Thiết bị không bị nóng lên khi sử dụng lâu.
- **Phản hồi nhanh:** Giảm độ trễ (latency) khi người dùng tương tác.

### 1.2. Cơ chế Rendering trong Jetpack Compose
Khác với hệ thống View truyền thống (XML), Jetpack Compose hoạt động qua 3 giai đoạn chính:
1.  **Composition (Cái gì?):** Chạy các hàm `@Composable` để xây dựng cây UI.
2.  **Layout (Ở đâu?):** Đo đạc kích thước và xác định vị trí các phần tử.
3.  **Drawing (Vẽ thế nào?):** Render các pixels lên màn hình (Canvas).

**Nguyên tắc tối ưu cốt lõi:** "Skip as much as possible" (Bỏ qua càng nhiều càng tốt). Chúng ta muốn tránh việc Compose phải chạy lại cả 3 giai đoạn khi chỉ có một thay đổi nhỏ.

### 1.3. Các vấn đề phổ biến
-   **Jank (Giật lag):** Khi một frame mất quá 16ms để render, frame đó bị bỏ qua (dropped frame), gây cảm giác khựng.
-   **Overdraw (Vẽ chồng):** Vẽ đi vẽ lại một pixel nhiều lần trong một frame.
-   **Recomposition (Tái cấu trúc thừa):** Các hàm Composable chạy lại không cần thiết dù dữ liệu đầu vào không đổi.

---

## CHƯƠNG 2: CÁC KỸ THUẬT TỐI ƯU GIAO DIỆN, ĐỒ HỌA

### 2.1. Kỹ thuật xử lý đồ họa (Graphics & Drawing)

#### a. Giảm GPU Overdraw
Đây là kỹ thuật quan trọng nhất để giảm tải cho GPU.
-   **Loại bỏ Background thừa:** Không đặt background màu cho các container cha nếu con đã che phủ hoàn toàn.
-   **Làm phẳng Layout (Flatten Hierarchy):** Giảm số lớp lồng nhau. Thay vì `Surface -> Box -> Column`, hãy thử dùng trực tiếp `Box` với modifier background.
-   **Tối ưu Transparency:** Tránh dùng `Color.Transparent` cho các wrapper như `Surface` hay `Card` nếu không cần thiết, vì chúng vẫn tạo ra một lệnh vẽ (draw call).
-   **Sử dụng `clip()` hợp lý:** Cắt bo góc (`RoundedCornerShape`) đúng vị trí để tránh vẽ phần thừa.

#### b. Tối ưu Animation và Image
-   **Modifier.graphicsLayer:** Sử dụng modifier này cho các animation như scale, alpha, rotation. Nó giúp thực hiện biến đổi trên GPU mà không kích hoạt lại giai đoạn Composition và Layout.
-   **Image Loading (Coil/Glide):** Luôn sử dụng thư viện tải ảnh bất đồng bộ, có caching bộ nhớ và ổ đĩa. Resize ảnh về kích thước hiển thị thực tế trước khi decode.

### 2.2. Kỹ thuật xử lý Logic giao diện (State & Composition)

#### a. Ổn định hóa dữ liệu (Stability)
-   **`@Immutable`:** Đánh dấu các Data Class là bất biến. Giúp Compose biết chắc chắn dữ liệu không đổi để bỏ qua Recomposition.
-   **`@Stable`:** Đánh dấu class có thể thay đổi nhưng có cơ chế thông báo cho Compose (như dùng `MutableState`).

#### b. Quản lý State thông minh
-   **`remember {}`:** Lưu trữ kết quả tính toán tốn kém, tránh tính lại mỗi lần recompose.
-   **`derivedStateOf {}`:** Chỉ recompose khi *kết quả* của một tính toán thay đổi, chứ không phải khi *đầu vào* thay đổi.
-   **`rememberSaveable {}`:** Lưu trạng thái qua các sự kiện hệ thống (xoay màn hình, kill process).

#### c. Tối ưu danh sách (Lazy Layouts)
-   **Sử dụng `key`:** Cung cấp ID duy nhất cho mỗi item trong `LazyColumn`/`LazyRow`. Giúp Compose định danh item khi thêm/xóa/sắp xếp, tránh vẽ lại toàn bộ danh sách.
-   **`contentType`:** Giúp tái sử dụng (recycle) các item component hiệu quả hơn nếu danh sách có nhiều loại view khác nhau.

---

## CHƯƠNG 3: CÁC CÔNG CỤ KIỂM TRA GIAO DIỆN VÀ ĐỒ HỌA

Để tối ưu hóa hiệu quả, cần sử dụng các công cụ đo lường chính xác.

### 3.1. Công cụ tích hợp trên thiết bị (Developer Options)

#### a. Debug GPU Overdraw
Công cụ trực quan nhất để phát hiện vẽ thừa.
-   **Cách bật:** Settings -> Developer Options -> Debug GPU Overdraw -> Show overdraw areas.
-   **Mã màu:**
    -   (Không màu): Vẽ 1 lần (Tuyệt vời).
    -   🔵 **Xanh dương:** Vẽ 2 lần (Tốt/Chấp nhận được).
    -   🟢 **Xanh lá:** Vẽ 3 lần (Cần xem xét).
    -   🔴 **Đỏ/Hồng:** Vẽ 4+ lần (Xấu, cần tối ưu ngay).

#### b. Profile GPU Rendering
Hiển thị biểu đồ thanh thời gian render của từng frame.
-   **Mục tiêu:** Giữ các thanh nằm dưới đường kẻ xanh lá (ngưỡng 16ms).

### 3.2. Công cụ trong Android Studio

#### a. Layout Inspector
-   Cho phép xem cây View/Composable 3D thời gian thực.
-   Kiểm tra xem bao nhiêu lớp đang chồng lên nhau.
-   Kích hoạt "Recomposition Counts" để xem số lần mỗi Composable bị vẽ lại.

#### b. Android Profiler
-   Theo dõi mức tiêu thụ CPU, Memory (RAM) và Energy (Pin) theo thời gian thực.
-   Phát hiện Memory Leaks (rò rỉ bộ nhớ) nếu đồ thị RAM tăng mãi không giảm.

#### c. Compose Compiler Metrics
-   Tạo báo cáo chi tiết về độ ổn định (Stability) của các class và hàm Composable. Giúp phát hiện tại sao một hàm bị recompose (Restartable, Skippable).

---

## CHƯƠNG 4: TRIỂN KHAI VÀ THỰC NGHIỆM

Dựa trên lý thuyết, chúng tôi đã áp dụng các kỹ thuật vào dự án **Gym App**.

### 4.1. Thực nghiệm 1: Sửa lỗi GPU Overdraw

**Vấn đề:** Màn hình `HomeScreen` và `WorkoutPlanScreen` hiển thị nhiều vùng màu xanh lá và hồng khi bật Debug Overdraw.
**Nguyên nhân:** Việc lồng ghép các Wrapper thừa thãi (`Surface` bọc `Box` có gradient).

**Giải pháp triển khai:**
1.  **HomeScreen (ModernTopBar):** Loại bỏ `Surface(color = Color.Transparent)`. Sử dụng trực tiếp `Box` với background gradient.
2.  **WorkoutPlanScreen (TodayPlanCard):** Loại bỏ `Card` wrapper bên ngoài, thay bằng `Box` có `clip(RoundedCornerShape)` và `shadow`.
3.  **WorkoutScreen:** Đổi màu nền `Scaffold` từ Transparent sang màu nền của Theme để tránh xung đột lớp vẽ.

**Kết quả:**
-   Chuyển từ màu Xanh lá/Hồng (3-4x overdraw) => Xanh dương (1-2x overdraw).
-   Giảm khoảng 20-30% lượng pixels phải vẽ lại trên màn hình chính.

### 4.2. Thực nghiệm 2: Tối ưu Recomposition

**Vấn đề:** Danh sách bài tập bị giật nhẹ khi cuộn hoặc khi cập nhật trạng thái một bài tập.
**Nguyên nhân:** Compose vẽ lại toàn bộ danh sách vì không phân biệt được item nào thay đổi, và data model bị coi là "Unstable".

**Giải pháp triển khai:**
1.  **Thêm `@Immutable`:** Áp dụng cho `WorkoutSession`, `WorkoutPlan`, `WorkoutSessionDetail` trong file `WorkoutModels.kt`.
2.  **LazyColumn Keys:**
    -   Code cũ: `items(exercises) { ... }`
    -   Code mới: `items(items = exercises, key = { it.id }) { ... }`
    -   Áp dụng tại `WorkoutScreen.kt` và `WorkoutPlanScreen.kt`.

**Kết quả:**
-   Khi tick chọn một bài tập là "Hoàn thành", chỉ item đó được vẽ lại. Các item khác giữ nguyên.
-   Tốc độ cuộn list mượt mà hơn.

### 4.3. Thực nghiệm 3: Quản lý Tài nguyên

**Vấn đề:** Video hướng dẫn trong `GuidanceScreen` có nguy cơ gây rò rỉ bộ nhớ.

**Giải pháp triển khai:**
-   Sử dụng `DisposableEffect` để quản lý vòng đời của `ExoPlayer`.
-   Tự động giải phóng (`release`) player khi người dùng rời màn hình hoặc đóng dialog.

**Kết quả:**
-   Không còn hiện tượng leak memory. Ứng dụng ổn định khi mở/đóng video nhiều lần.

---

**KẾT LUẬN:**
Việc kết hợp sửa lỗi đồ họa (Overdraw) và tối ưu logic (Recomposition/Stability) đã giúp Gym App hoạt động hiệu quả hơn đáng kể. Đây là quy trình cần được thực hiện liên tục trong vòng đời phát triển ứng dụng.

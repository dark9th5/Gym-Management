# Danh Sách Thay Đổi Tối Ưu Hóa Giao Diện & Đồ Họa

Tài liệu này liệt kê chi tiết các thay đổi đã được áp dụng trong mã nguồn dự án **Gym App** nhằm mục tiêu tối ưu hóa hiệu năng, giảm thiểu Overdraw và nâng cao trải nghiệm người dùng.

---

## 1. Tối Ưu Hóa Overdraw (Vẽ Chồng)

Mục tiêu: Loại bỏ các lớp background thừa thãi, đảm bảo GPU không phải vẽ lại nhiều lần trên cùng một pixel.

### 1.1. Cấp độ Theme (`themes.xml`)
*   **File:** `app/src/main/res/values/themes.xml`
*   **Thay đổi:**
    *   Set thuộc tính `android:windowBackground` thành `@null` (thay vì màu trắng mặc định).
*   **Giải thích:** Các màn hình (Screen) trong Compose đều tự vẽ background riêng (ví dụ gradient hoặc màu solid). Việc để window vẽ thêm một lớp màu trắng bên dưới là thừa thãi và gây overdraw 1x cho toàn bộ ứng dụng.

### 1.2. Màn hình Trang chủ (`HomeScreen.kt`)
*   **File:** `app/src/main/java/com/lc9th5/gym/ui/view/HomeScreen.kt`
*   **Thay đổi:**
    *   Loại bỏ component `Surface` bao bọc bên ngoài `ModernTopBar`.
    *   Chuyển các thuộc tính background trực tiếp vào `Box` container.
*   **Giải thích:** `Surface` dù trong suốt vẫn tạo ra một layer vẽ. Bằng cách xóa nó và vẽ background trực tiếp lên `Box`, ta giảm được 1 lớp overdraw cho khu vực TopBar.

### 1.3. Màn hình Tập luyện (`WorkoutScreen.kt`)
*   **File:** `app/src/main/java/com/lc9th5/gym/ui/view/WorkoutScreen.kt`
*   **Thay đổi:**
    *   Loại bỏ cấu trúc `Box` lồng bên ngoài `Scaffold`.
    *   Sử dụng tham số `floatingActionButton` của `Scaffold` thay vì tự căn chỉnh FAB bằng `Box`.
    *   Đặt `containerColor` của `Scaffold` thành `Color.Transparent`.
*   **Giải thích:**
    *   `Scaffold` mặc định có background màu trắng. Khi đặt nó đè lên `HomeScreen` (đã có background), ta bị overdraw. Việc set `Transparent` giúp tận dụng background của màn hình cha.
    *   Việc dùng slot FAB chuẩn của Scaffold giúp hệ thống tối ưu hóa việc vẽ layer float, tránh việc phải tạo `Box` cha chỉ để chứa layout.

---

## 2. Tối Ưu Hóa Recomposition (Vẽ Lại)

Mục tiêu: Giảm thiểu số lần Compose phải tính toán và vẽ lại UI khi dữ liệu thay đổi.

### 2.1. Sử dụng Smart Keys (`WorkoutScreen.kt`)
*   **File:** `app/src/main/java/com/lc9th5/gym/ui/view/WorkoutScreen.kt`
*   **Thay đổi:**
    *   Thêm tham số `key` vào hàm `items` của `LazyColumn`.
    *   Sử dụng ID duy nhất (ví dụ: `exercise.id`) làm key.
*   **Giải thích:** Khi không có key, nếu thứ tự bài tập thay đổi hoặc có bài tập bị xóa, Compose có thể phải vẽ lại toàn bộ danh sách. Key giúp Compose nhận biết chính xác item nào đã thay đổi để chỉ vẽ lại item đó.

### 2.2. Immutable Data Models (`WorkoutModels.kt`)
*   **File:** `app/src/main/java/com/lc9th5/gym/data/model/WorkoutModels.kt`
*   **Thay đổi:**
    *   Thêm annotation `@androidx.compose.runtime.Immutable` cho các data class chính như `WorkoutSession`, `WorkoutPlan`.
*   **Giải thích:** Các class chứa `List` thường được Compose coi là "không ổn định" (unstable). Annotation này cam kết với trình biên dịch rằng dữ liệu sẽ không thay đổi ngầm, cho phép Compose bỏ qua việc kiểm tra thay đổi (Skipping Recomposition) cho các UI component nhận data này.

---

## 3. Nâng Cao Hiệu Năng Cảm Nhận (Perceived Performance)

Mục tiêu: Làm cho ứng dụng *cảm thấy* nhanh và mượt hơn trong mắt người dùng, dù tốc độ xử lý thực tế không đổi.

### 3.1. Skeleton Loading (`GuidanceScreen.kt`)
*   **File:** `app/src/main/java/com/lc9th5/gym/ui/view/GuidanceScreen.kt`
*   **Thay đổi:**
    *   Triển khai `LessonSkeletonCell` thay vì dùng `CircularProgressIndicator`.
*   **Giải thích:** Hiển thị khung xương cấu trúc nội dung (với hiệu ứng pulse) giúp người dùng giảm cảm giác chờ đợi và hình dung được bố cục sắp xuất hiện, tạo trải nghiệm mượt mà hơn so với vòng xoay vô tận.

### 3.2. Smooth Transitions (`HomeScreen.kt`)
*   **File:** `app/src/main/java/com/lc9th5/gym/ui/view/HomeScreen.kt`
*   **Thay đổi:**
    *   Sử dụng `AnimatedContent` với hiệu ứng `fadeIn` / `fadeOut` cho việc chuyển đổi Tab.
*   **Giải thích:** Việc chuyển đổi nội dung đột ngột (cắt cảnh) gây cảm giác giật cục. Hiệu ứng mờ dần giúp mắt người dùng dễ chịu hơn và che lấp các khoảng trễ nhỏ trong quá trình render màn hình mới.

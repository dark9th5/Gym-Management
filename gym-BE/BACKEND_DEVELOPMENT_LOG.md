# � MASTER LOG: QUY TRÌNH PHÁT TRIỂN BACKEND GYM-MANAGEMENT

Tài liệu này hệ thống lại toàn bộ các bước lập trình, nguyên nhân và các file mã nguồn tương ứng để bạn nắm rõ cấu trúc hệ thống.

---

## 🏗️ BƯỚC 1: KHỞI TẠO NỀN TẢNG (INFRASTRUCTURE)
**Mục tiêu:** Thiết lập "móng nhà" cho dự án.
*   **Nguyên nhân:** Cần một framework mạnh mẽ (Spring Boot), ngôn ngữ hiện đại (Kotlin) và công cụ build tự động (Gradle).
*   **Các file thực hiện:**
    *   `build.gradle.kts`: Cấu hình thư viện (Spring Boot, JPA, Security, Gemini AI, MySQL).
    *   `src/main/resources/application.properties`: Thông số kết nối Database, Mail Server, JWT Secret Key.
    *   `GymBeApplication.kt`: File chạy chính của server.
*   **Kết quả:** Server có khả năng khởi chạy và kết nối thành công với Database MySQL.

---

## �️ BƯỚC 2: THIẾT KẾ CƠ SỞ DỮ LIỆU (DATABASE MODELING)
**Mục tiêu:** Định nghĩa các thực thể dữ liệu cần quản lý.
*   **Nguyên nhân:** Dữ liệu GYM cần có cấu trúc phân tầng (User -> Session -> Exercise -> Set).
*   **Các file thực hiện (Thư mục `model/`):**
    *   `user/User.kt`: Lưu thông tin tài khoản, email, mật khẩu (đã hash) và 2FA.
    *   `workout/WorkoutSession.kt`: Đại diện cho một buổi tập.
    *   `workout/WorkoutExercise.kt` & `WorkoutSet.kt`: Chi tiết từng bài tập và hiệp tập.
*   **Kết quả:** Hệ thống JPA tự động sinh ra các bảng tương ứng trong Database.

---

## � BƯỚC 3: BẢO MẬT VÀ XÁC THỰC (SECURITY & AUTH)
**Mục tiêu:** Bảo vệ API khỏi các truy cập trái phép.
*   **Nguyên nhân:** Dữ liệu người dùng cần được bảo vệ qua Token (JWT) và bảo mật 2 lớp (2FA).
*   **Các file thực hiện:**
    *   `config/SecurityConfig.kt`: "Người gác cổng" cho phép hoặc chặn các Request.
    *   `config/JwtConfig.kt` & `service/TokenService.kt`: Logic tạo ra "vé thông hành" (JWT) cho User.
    *   `service/TotpService.kt` & `controller/TwoFactorAuthController.kt`: Xử lý mã OTP 6 số từ Google Authenticator.
*   **Kết quả:** Người dùng phải đăng nhập mới có thể xem/sửa dữ liệu tập luyện.

---

## 🏋️ BƯỚC 4: LOGIC NGHIỆP VỤ CỐT LÕI (CORE BUSINESS LOGIC)
**Mục tiêu:** Xử lý các chức năng chính của ứng dụng.
*   **Nguyên nhân:** Cần tách biệt việc xử lý dữ liệu (Service) khỏi việc nhận yêu cầu (Controller).
*   **Các file thực hiện (Thư mục `service/`):**
    *   `workout/WorkoutSessionService.kt`: Xử lý logic bắt đầu buổi tập, kết thúc và tính thời gian.
    *   `workout/StreakService.kt`: Theo dõi chuỗi ngày tập liên tiếp để tạo động lực.
    *   `util/EncryptionService.kt` & `WorkoutEncryptionHelper.kt`: Mã hóa dữ liệu nhạy cảm bằng AES-256-GCM trước khi lưu.
*   **Kết quả:** Dữ liệu được tính toán chính xác và lưu trữ bảo mật dưới dạng đã mã hóa.

---

## 🤖 BƯỚC 5: TÍCH HỢP AI CHATBOT (INTELLIGENCE)
**Mục tiêu:** Tạo trợ lý ảo tư vấn Gym ngay trong App.
*   **Nguyên nhân:** Giúp người dùng có PT riêng 24/7 mà không cần thoát app ra ngoài.
*   **Các file thực hiện:**
    *   `service/ChatbotService.kt`: Kết nối Google Gemini API, xây dựng System Prompt (PT tính cách chuyên nghiệp).
    *   `controller/ChatbotController.kt`: Đầu cuối nhận câu hỏi từ App Android gửi lên.
*   **Kết quả:** Chatbot có thể trả lời các câu hỏi về bài tập, dinh dưỡng và kỹ thuật Gym.

---

## � BƯỚC 6: XÂY DỰNG GIAO DIỆN API (CONTROLLER LAYER)
**Mục tiêu:** Điểm tiếp nhận yêu cầu từ Mobile App.
*   **Nguyên nhân:** Cần các Endpoint rõ ràng cho Android gọi lên.
*   **Các file thực hiện (Thư mục `controller/`):**
    *   `AuthController.kt`: Đăng nhập/Đăng ký.
    *   `workout/WorkoutSessionController.kt`: Truy xuất dữ liệu tập luyện.
    *   `workout/StreakController.kt`: Lấy thông tin về chuỗi ngày tập.
*   **Kết quả:** App Android có thể giao tiếp với Backend qua các URL chuẩn RESTful API.

---

## 🚀 BƯỚC 7: TRIỂN KHAI VÀ TUNNELING (DEPLOYMENT)
**Mục tiêu:** Đưa ứng dụng ra Internet để Mobile App kết nối được.
*   **Công cụ:** **Ngrok**.
*   **Nguyên nhân:** Backend chạy ở máy local cần một đường hầm bảo mật (HTTPS) để điện thoại kết nối từ mọi nơi.
*   **Kết quả:** Toàn bộ dữ liệu truyền tải giữa App và Server được mã hóa TLS/SSL qua đường hầm Ngrok.

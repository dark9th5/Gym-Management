# 📘 NHẬT KÝ PHÁT TRIỂN BACKEND TOÀN DIỆN (GYM APP)

Tài liệu này mô tả chi tiết quy trình xây dựng Backend từ con số 0 đến khi hoàn thiện, bao gồm luồng dữ liệu, cấu trúc code và lý do kỹ thuật cho từng quyết định.

---

## 🚀 GIAI ĐOẠN 1: KHỞI TẠO & CẤU HÌNH NỀN TẢNG (FOUNDATION)

**Mục tiêu:** Xây dựng khung sườn vững chắc, kết nối Database, và thiết lập môi trường build.

### 📍 Bước 1.1: Khởi tạo Project Spring Boot
- **Công cụ:** Spring Initializr.
- **Ngôn ngữ:** Kotlin (JVM 21) - Chọn vì tính ngắn gọn, an toàn null-safety.
- **Build Tool:** Gradle (Kotlin DSL).
- **Dependencies chính:**
    - `spring-boot-starter-web`: Xây dựng RESTful API.
    - `spring-boot-starter-data-jpa`: Tương tác Database (Hibernate).
    - `spring-boot-starter-security`: Bảo mật Auth/Authz.
    - `mysql-connector-j`: Driver kết nối MySQL.

**📄 Code: `build.gradle.kts`**
```kotlin
plugins {
    id("org.springframework.boot") version "3.5.6"
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20" // Hỗ trợ open class cho Spring AOP
    kotlin("plugin.jpa") version "2.2.20"    // Hỗ trợ no-arg constructor cho Entity
}
// ... cấu hình dependencies
```

### 📍 Bước 1.2: Cấu hình Database & App Properties
- **File:** `application.properties` (hoặc `.yml`).
- **Nội dung:** Kết nối MySQL, cấu hình JPA, Mail Server (Gmail SMTP), và các biến môi trường cho Security (JWT Key, Google Client ID).

---

## 🔐 GIAI ĐOẠN 2: HỆ THỐNG XÁC THỰC & BẢO MẬT (AUTH & SECURITY)

**Mục tiêu:** Đảm bảo chỉ người dùng uy tín mới được truy cập dữ liệu. Kích hoạt tính năng bảo mật 2 lớp (2FA).

### 📍 Bước 2.1: Model User & Role
- **Entity `User`:** Lưu username, email, password (hashed), trạng thái verify, và các trường tin cho 2FA (`totpSecret`, `backupCodes`).
- **Repository:** `UserRepository` để query user theo email/username.

**📄 Code: `User.kt`**
```kotlin
@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val email: String,
    
    @Column(name = "totp_secret")
    var totpSecret: String? = null, // Key cho Google Authenticator
    
    @Column(name = "is_2fa_enabled")
    var is2faEnabled: Boolean = false
    // ...
)
```

### 📍 Bước 2.2: Cấu hình Spring Security & JWT
- **Logic:**
    1.  User login -> Server xác thực -> Trả về Access Token & Refresh Token.
    2.  Client gửi Request + Token -> Server validate Token -> Cho phép truy cập tài nguyên.
- **File `SecurityConfig.kt`:**
    - `PasswordEncoder`: Dùng BCrypt (độ mạnh 12).
    - `SecurityFilterChain`: Cấu hình public endpoint (`/auth/**`) và private endpoint (`/api/**`).
    - Tắt CSRF (vì dùng Stateless API), bật CORS.

### 📍 Bước 2.3: Triển khai 2FA (TOTP)
- **Công cụ:** Thư viện `dev.samstevens.totp`.
- **Luồng:**
    1.  API `/auth/2fa/setup`: Server tạo Secret -> Trả về QR Code.
    2.  User quét QR bằng Google Auth app.
    3.  API `/auth/2fa/verify`: User nhập mã 6 số -> Server verify -> Bật 2FA.
    4.  Khi Login: Nếu user có 2FA -> Trả về `PRE_AUTH_TOKEN` -> User nhập OTP -> Trả về `FULL_TOKEN`.

---

## 🏋️ GIAI ĐOẠN 3: LOGIC CỐT LÕI - QUẢN LÝ TẬP LUYỆN (CORE DOMAIN)

**Mục tiêu:** Quản lý toàn bộ vòng đời của việc tập luyện: Kế hoạch -> Buổi tập -> Bài tập -> Sets.

### 📍 Bước 3.1: Thiết kế Data Model (Entity Relationship)
- **Cấu trúc:**
    - `WorkoutPlan` (1) ↔ (N) `WorkoutPlanDay` (Ngày tập trong kế hoạch).
    - `WorkoutSession` (1) ↔ (N) `WorkoutExercise` (Bài tập trong buổi).
    - `WorkoutExercise` (1) ↔ (N) `WorkoutSet` (Hiệp tập: Reps, Kg).

### 📍 Bước 3.2: Bảo mật dữ liệu nhạy cảm (Data Encryption)
- **Vấn đề:** User muốn ghi chú riêng tư, tên bài tập, chỉ số cơ thể phải được bảo mật tuyệt đối.
- **Giải pháp:** Mã hóa AES-256-GCM trước khi lưu xuống DB.
- **Service:** `EncryptionService.kt` & `WorkoutEncryptionHelper.kt`.

**📄 Code: `EncryptionService.kt`**
```kotlin
fun encrypt(plainText: String): String {
    val iv = ByteArray(12) // Random IV cho mỗi lần encrypt
    secureRandom.nextBytes(iv)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    // ... encrypt logic ...
    return Base64.encode(iv + cipherText) // Kết hợp IV để decrypt sau này
}
```

### 📍 Bước 3.3: Quản lý Buổi tập (Session Service)
- **Logic:**
    - `startSession()`: Tạo record mới với `startedAt`.
    - `addExercise()`: Thêm bài tập vào buổi.
    - `endSession()`: Tính duration = `now - startedAt`, cập nhật Streak.
    - **Transaction:** Dùng `@Transactional` để đảm bảo lưu Session + Exercises + Sets thành công đồng thời.

---

## 📈 GIAI ĐOẠN 4: GAMIFICATION & STATISTICS

**Mục tiêu:** Tạo động lực bằng chuỗi ngày tập (Streak) và biểu đồ thống kê.

### 📍 Bước 4.1: Hệ thống Streak
- **Logic cập nhật Streak (`StreakService.kt`):**
    - Lấy ngày tập gần nhất (`lastWorkoutDate`).
    - So sánh với hôm nay:
        - `diff == 1`: Tăng streak (+1).
        - `diff > 1`: Reset streak về 1.
        - `diff == 0`: Giữ nguyên (đã tập hôm nay rồi).

### 📍 Bước 4.2: Thống kê (Statistics Service)
- **API:** `/api/stats/dashboard`.
- **Dữ liệu:** Tổng số buổi tập, tổng thời gian, calo tiêu thụ, biểu đồ cân nặng theo thời gian.
- **Query:** Sử dụng JPQL Custom Query trong Repository để aggregate dữ liệu hiệu quả.

---

## 🤖 GIAI ĐOẠN 5: TÍCH HỢP TRÍ TUỆ NHÂN TẠO (GYM AI CHATBOT)

**Mục tiêu:** Trợ lý ảo trả lời câu hỏi chuyên sâu về Gym.

### 📍 Bước 5.1: Cấu hình Gemini API
- **Client:** Sử dụng `WebClient` (Reactive Stack) để gọi Google Gemini API bất đồng bộ.
- **System Prompt:** Định nghĩa "tính cách" cho AI: "Bạn là PT chuyên nghiệp, thân thiện, chỉ trả lời về Gym/Dinh dưỡng".

### 📍 Bước 5.2: Tối ưu & Rate Limiting
- **Bộ lọc từ khóa:** Kiểm tra input user có chứa từ khóa liên quan (`gym`, `protein`, `tập`, `đau cơ`...) trước khi gọi API -> Tiết kiệm quota.
- **Rate Limit:** Giới hạn 15 request/phút. Thêm delay nhân tạo nếu user spam.
- **Fallback:** Nếu API lỗi/hết quota -> Trả về các câu tips có sẵn (hardcoded) xoay vòng.

**📄 Code: `ChatbotService.kt`**
```kotlin
if (!isGymRelated(message)) {
    return "Xin lỗi, tôi chỉ là chuyên gia Gym thôi ạ! 🏋️"
}
// Gọi Gemini API...
```

---

## 📧 GIAI ĐOẠN 6: TIỆN ÍCH & TRIỂN KHAI (UTILITIES & DEPLOY)

**Mục tiêu:** Hoàn thiện hệ thống hỗ trợ và đưa vào hoạt động.

### 📍 Bước 6.1: Email Verification
- **Flow:** User đăng ký -> Sinh token ngẫu nhiên -> Gửi email chứa link `/verify?token=xyz`.
- **Cleanup:** Chạy Scheduled Task (`@Scheduled`) mỗi đêm để xóa các tài khoản chưa verify sau 24h.

### 📍 Bước 6.2: Triển khai Local & Tunneling
- **Môi trường:** Máy cá nhân Windows.
- **Database:** MySQL Local.
- **Public Internet:** Sử dụng **Ngrok** để expose cổng 8080 ra Internet, giúp Mobile App (trên 4G/Wifi khác) có thể kết nối vào Backend local.

---

## ✅ KẾT LUẬN & KẾT QUẢ ĐẠT ĐƯỢC

Sau quá trình phát triển, hệ thống Backend **Gym-Management** đã đạt được:
1.  **Bảo mật:** Authentication chuẩn công nghiệp (OAuth2/JWT) + 2FA + Mã hóa dữ liệu AES.
2.  **Thông minh:** AI Chatbot tích hợp thực sự hữu ích cho người tập.
3.  **Hiệu năng:** Database được tối ưu index, caching ở mức Service.
4.  **Trải nghiệm người dùng:** Hệ thống Streak và Thống kê tạo động lực mạnh mẽ.
5.  **Clean Code:** Cấu trúc phân tầng rõ ràng (Controller -> Service -> Repo -> Model).

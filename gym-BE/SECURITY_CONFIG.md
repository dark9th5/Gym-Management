# 🔐 HƯỚNG DẪN CẤU HÌNH BẢO MẬT - GYM APP

## Tổng quan các tính năng bảo mật đã triển khai

### ✅ 1. Mã hóa dữ liệu lưu trữ & phiên làm việc

| Thành phần | Thuật toán | Vị trí |
|------------|------------|--------|
| Token trên Android | **AES-256-GCM** | `TokenManager.kt` (EncryptedSharedPreferences) |
| Mật khẩu trong DB | **BCrypt (12 rounds)** | `SecurityConfig.kt` |
| Tin nhắn Chat | **AES-256-GCM** | `EncryptionService.kt` (MỚI) |
| JWT Token | **HMAC-SHA256** | `JwtConfig.kt` |

### ✅ 2. Xác thực & Phân quyền

| Tính năng | Mô tả | Vị trí |
|-----------|-------|--------|
| **OTP Email** | Gửi mã 6 ký tự khi đăng ký | `UserService.kt` |
| **2FA TOTP** | Google Authenticator compatible | `TotpService.kt` |
| **JWT Auth** | Access + Refresh tokens | `TokenService.kt` |
| **Role-based** | USER/ADMIN roles | `SecurityConfig.kt` |
| **Token Blacklist** | Vô hiệu hóa token khi logout | `BlacklistedToken.kt` |

### ✅ 3. Chống tấn công

| Tấn công | Biện pháp | Vị trí |
|----------|-----------|--------|
| **SQL Injection** | JPA Parameterized Queries | Tất cả Repository |
| **XSS** | Input Sanitization | `XssSanitizer.kt` (MỚI) |
| **Brute Force** | Rate limiting (5 attempts/15min) | `LoginAttemptService.kt` |
| **Password Cracking** | BCrypt 12 rounds | `SecurityConfig.kt` |

### ✅ 4. Bảo mật đường truyền

| Tính năng | Mô tả | Vị trí |
|-----------|-------|--------|
| **TLS 1.2/1.3** | Enforce modern TLS | `ApiClient.kt` |
| **Certificate Pinning** | Production only | `ApiClient.kt` |
| **HTTPS Required** | All production URLs | `ServerConfig.kt` |

### ✅ 5. Bảo vệ mã nguồn

| Tính năng | Mô tả | Vị trí |
|-----------|-------|--------|
| **ProGuard/R8** | Code obfuscation | `build.gradle.kts` |
| **Root Detection** | Chặn thiết bị root | `SecurityUtils.kt` |
| **Emulator Detection** | Phát hiện emulator | `SecurityUtils.kt` |

---

## 🔑 CẤU HÌNH ENCRYPTION KEY

### Bước 1: Tạo encryption key mới

Chạy lệnh sau để tạo key AES-256 (chỉ cần làm 1 lần):

```kotlin
// Kotlin REPL hoặc main function
import java.security.SecureRandom
import java.util.Base64

fun main() {
    val keyBytes = ByteArray(32) // 256 bits
    SecureRandom().nextBytes(keyBytes)
    val key = Base64.getEncoder().encodeToString(keyBytes)
    println("Generated ENCRYPTION_SECRET_KEY: $key")
}
```

Hoặc dùng OpenSSL:
```bash
openssl rand -base64 32
```

### Bước 2: Cấu hình environment variable

**Windows PowerShell:**
```powershell
$env:ENCRYPTION_SECRET_KEY="YOUR_GENERATED_KEY_HERE"
```

**Linux/macOS:**
```bash
export ENCRYPTION_SECRET_KEY="YOUR_GENERATED_KEY_HERE"
```

**Hoặc trong application.properties:**
```properties
encryption.secret.key=YOUR_GENERATED_KEY_HERE
```

### Bước 3: Verify cấu hình

Chạy backend và kiểm tra log không có lỗi "ENCRYPTION_SECRET_KEY không được cấu hình".

---

## 📊 MINH HỌA MÃ HÓA TIN NHẮN

### Trước khi mã hóa (Database lưu plaintext - KHÔNG AN TOÀN):
```
| id | user_id | content                          | is_from_user |
|----|---------|----------------------------------|--------------|
| 1  | 123     | Tôi muốn giảm cân                | true         |
| 2  | 123     | Để giảm cân, bạn nên tập cardio  | false        |
```

### Sau khi mã hóa (Database lưu ciphertext - AN TOÀN):
```
| id | user_id | content                                              | is_from_user |
|----|---------|------------------------------------------------------|--------------|
| 1  | 123     | dGhpcyBpcyBlbmNyeXB0ZWQgY29udGVudA==... (Base64)     | true         |
| 2  | 123     | YW5vdGhlciBlbmNyeXB0ZWQgbWVzc2FnZQ==... (Base64)     | false        |
```

### Flow mã hóa/giải mã:
```
[User Input]             [Sanitize XSS]         [Encrypt AES-256]        [Save to DB]
     |                         |                        |                      |
     v                         v                        v                      v
"<script>hack</script>"  "[removed]Hi"     "e3R5cGU6ImVuYy..."     INSERT INTO...

[Read from DB]           [Decrypt AES-256]       [Return to Client]
     |                         |                        |
     v                         v                        v
"e3R5cGU6ImVuYy..."     "Hi there!"              JSON Response
```

---

## 🛡️ CHECKLIST BẢO MẬT

- [x] EncryptedSharedPreferences cho Android
- [x] BCrypt password hashing
- [x] JWT với HMAC-SHA256
- [x] 2FA TOTP support
- [x] Email OTP verification
- [x] Rate limiting chống brute force
- [x] TLS 1.2+ enforcement
- [x] Certificate pinning (production)
- [x] XSS sanitization
- [x] AES-256-GCM message encryption
- [x] Root/Emulator detection
- [x] ProGuard code obfuscation
- [x] Token blacklisting
- [x] SQL Injection prevention (JPA)

---

## 📁 FILES BẢO MẬT

### Backend (gym-BE):
- `util/EncryptionService.kt` - Mã hóa AES-256-GCM
- `util/XssSanitizer.kt` - Chống XSS
- `service/LoginAttemptService.kt` - Rate limiting
- `service/TotpService.kt` - 2FA TOTP
- `service/TokenService.kt` - JWT management
- `config/SecurityConfig.kt` - Spring Security
- `config/JwtConfig.kt` - JWT encoding/decoding

### Android (gym-Android):
- `data/local/TokenManager.kt` - Encrypted token storage
- `utils/SecurityUtils.kt` - Root/Emulator detection
- `data/remote/ApiClient.kt` - TLS & Certificate pinning
- `util/Validator.kt` - Input validation

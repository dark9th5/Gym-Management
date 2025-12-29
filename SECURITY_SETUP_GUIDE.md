# 🔒 Hướng Dẫn Cấu Hình Bảo Mật

## Mục Lục
1. [Backend (gym-BE)](#backend-gym-be)
2. [Android (gym-Android)](#android-gym-android)
3. [Tạo Keystore](#tạo-keystore-cho-android)

---

## Backend (gym-BE)

### Environment Variables Cần Thiết

Tạo file `.env` hoặc set environment variables:

```bash
# Database
DB_PASSWORD=your_database_password_here

# JWT (bắt buộc, min 32 ký tự)
JWT_SECRET=your_jwt_secret_minimum_32_characters_here

# Encryption (bắt buộc, đúng 32 ký tự)
ENCRYPTION_SECRET_KEY=your_32_character_encryption_key

# Email
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Gemini AI (optional)
GEMINI_API_KEY=your_gemini_api_key
```

### Chạy Backend với Environment Variables

**Windows (PowerShell):**
```powershell
$env:DB_PASSWORD="Lucvip2003@"
$env:JWT_SECRET="your_super_secret_jwt_key_min_32_chars"
$env:ENCRYPTION_SECRET_KEY="your32characterencryptionkey!!"
./gradlew bootRun
```

**Windows (CMD):**
```cmd
set DB_PASSWORD=Lucvip2003@
set JWT_SECRET=your_super_secret_jwt_key_min_32_chars
set ENCRYPTION_SECRET_KEY=your32characterencryptionkey!!
gradlew bootRun
```

**Linux/Mac:**
```bash
export DB_PASSWORD="Lucvip2003@"
export JWT_SECRET="your_super_secret_jwt_key_min_32_chars"
export ENCRYPTION_SECRET_KEY="your32characterencryptionkey!!"
./gradlew bootRun
```

---

## Android (gym-Android)

### Signing Configuration

Thêm vào `local.properties` hoặc set environment variables:

```properties
# Keystore configuration
KEYSTORE_PATH=path/to/your/keystore.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

### Build Release APK

```bash
./gradlew assembleRelease
```

---

## Tạo Keystore cho Android

### Bước 1: Tạo Keystore

```bash
keytool -genkey -v -keystore gym-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias gym
```

Làm theo hướng dẫn để nhập:
- Keystore password
- Key password (có thể giống keystore password)
- Thông tin organization

### Bước 2: Lấy SHA-256 Signature

```bash
keytool -list -v -keystore gym-release.jks -alias gym
```

Copy giá trị SHA-256 và cập nhật vào `SecurityUtils.kt`:

```kotlin
private val EXPECTED_SIGNATURES: Set<String>? = setOf(
    "YOUR_SHA256_SIGNATURE_HERE"
)
```

### Bước 3: Cấu hình trong local.properties

```properties
KEYSTORE_PATH=D:/path/to/gym-release.jks
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=gym
KEY_PASSWORD=your_password
```

---

## Checklist Bảo Mật

### ✅ Backend
- [x] Database password dùng env variable
- [x] JWT secret dùng env variable
- [x] Encryption key dùng env variable
- [x] Mail credentials dùng env variables
- [x] application.properties trong .gitignore
- [x] Có application.properties.example làm template

### ✅ Android
- [x] ProGuard/R8 minification enabled
- [x] shrinkResources enabled
- [x] ProGuard rules đầy đủ
- [x] Signing config cho release
- [x] Root detection (5 methods)
- [x] Emulator detection (comprehensive)
- [x] Anti-tampering (signature verification)
- [x] Debugger detection
- [x] Frida detection
- [x] String encryption utility
- [x] Keystore files trong .gitignore

---

## Điểm Bảo Mật Mới: 8/10 ⬆️

| Kỹ thuật | Trước | Sau |
|----------|-------|-----|
| ProGuard/R8 Minification | ✅ Cơ bản | ✅ Tối ưu |
| ProGuard Rules | ⚠️ Thiếu | ✅ Đầy đủ |
| Code Shrinking | ❌ Chưa có | ✅ Có |
| Code Signing | ⚠️ Chưa config | ✅ Có config |
| String Encryption | ❌ Chưa có | ✅ Có utility |
| Root Detection | ✅ Cơ bản | ✅ Nâng cao (5 methods) |
| Emulator Detection | ✅ Cơ bản | ✅ Nâng cao |
| Anti-Tampering | ❌ Chưa có | ✅ Có |
| Debugger Detection | ❌ Chưa có | ✅ Có |
| Frida Detection | ❌ Chưa có | ✅ Có |

---

## Lưu Ý Quan Trọng

1. **KHÔNG BAO GIỜ** commit file `application.properties` chứa secrets
2. **KHÔNG BAO GIỜ** commit file keystore (.jks, .keystore)
3. Backup keystore và passwords ở nơi an toàn
4. Sử dụng các giá trị khác nhau cho dev/staging/production
5. Định kỳ rotate secrets và API keys

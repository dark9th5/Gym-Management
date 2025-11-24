# 🐛 Tổng Hợp Lỗi Đã Sửa - Android App

## ✅ Các lỗi đã được sửa:

### 🔴 Lỗi 1: Thiếu INTERNET Permission
**Vấn đề:** App không thể kết nối mạng vì thiếu quyền INTERNET
**Giải pháp:** Đã thêm vào `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 🔴 Lỗi 2: Sử dụng localhost trong ApiClient
**Vấn đề:** `localhost` trong Android không trỏ đến máy tính host
**Giải pháp:** Đã đổi thành `10.0.2.2:8080` cho Android Emulator

**Lưu ý quan trọng:**
- Nếu chạy trên **Android Emulator**: Dùng `http://10.0.2.2:8080` ✅
- Nếu chạy trên **thiết bị thật**: Cần đổi thành IP thực của máy tính (ví dụ: `http://192.168.1.100:8080`)

### 🔴 Lỗi 3: Thiếu usesCleartextTraffic
**Vấn đề:** Android 9+ chặn HTTP traffic mặc định
**Giải pháp:** Đã thêm `android:usesCleartextTraffic="true"` vào `<application>`

⚠️ **Chú ý bảo mật:** Trong production nên dùng HTTPS thay vì HTTP!

### 🔴 Lỗi 4: Xử lý Response không đúng
**Vấn đề:** API login trả về `Response<Any>` nhưng code parse trực tiếp thành `AuthResponse`
**Giải pháp:** Đã thêm Gson để parse:
```kotlin
val jsonString = gson.toJson(response.body())
val authResponse = gson.fromJson(jsonString, AuthResponse::class.java)
```

### 🔴 Lỗi 5: VerifyState.Success không nhận tham số
**Vấn đề:** `VerifyState.Success` định nghĩa là `object` nhưng code lại truyền tham số vào
**Giải pháp:** Đã đổi thành `data class Success(val message: String)`

### 🔴 Lỗi 6: Thiếu thư viện Logging Interceptor
**Vấn đề:** `build.gradle.kts` dùng `libs.okhttpLoggingInterceptor` nhưng không được định nghĩa
**Giải pháp:** Đã thêm vào `libs.versions.toml`:
```toml
okhttpLoggingInterceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttpLogging" }
```

## 🚀 Cách kiểm tra Backend đang chạy

Trước khi chạy app Android, hãy đảm bảo backend Spring Boot đang chạy:

```bash
cd myfamily-BE
./gradlew bootRun
```

Backend sẽ chạy tại: `http://localhost:8080`

## 📱 Cách chạy App Android

### Với Android Emulator:
1. Mở Android Studio
2. Start một Android Emulator
3. Chạy app (Shift+F10 hoặc nút ▶️ Run)
4. App sẽ tự động kết nối tới backend qua `http://10.0.2.2:8080`

### Với thiết bị thật:
1. Tìm IP của máy tính (trong Windows PowerShell: `ipconfig`)
2. Mở file `ApiClient.kt`
3. Đổi `BASE_URL` thành: `"http://YOUR_COMPUTER_IP:8080"`
   Ví dụ: `"http://192.168.1.100:8080"`
4. Đảm bảo điện thoại và máy tính cùng mạng WiFi
5. Chạy app

## 🔧 Các bước sau khi sửa

1. **Sync Gradle:** File → Sync Project with Gradle Files
2. **Clean Build:** Build → Clean Project
3. **Rebuild:** Build → Rebuild Project
4. **Run:** Chạy app và test các tính năng đăng ký/đăng nhập

## 📋 Checklist Test

- [ ] Backend đang chạy tại port 8080
- [ ] Mở app và thấy màn hình Login
- [ ] Nhấn "Register" để tạo tài khoản mới
- [ ] Điền thông tin và đăng ký
- [ ] Nhập mã xác thực từ email
- [ ] Đăng nhập bằng tài khoản vừa tạo

## ⚠️ Troubleshooting

**Lỗi: "Unable to resolve host"**
→ Kiểm tra lại IP address trong `ApiClient.kt`

**Lỗi: "Connection refused"**
→ Đảm bảo backend đang chạy tại port 8080

**Lỗi: "Network security policy"**
→ Đã được sửa bằng `usesCleartextTraffic="true"`

## 📚 Tài liệu tham khảo

- [Android Network Security Configuration](https://developer.android.com/training/articles/security-config)
- [Retrofit Documentation](https://square.github.io/retrofit/)
- [Android Emulator Networking](https://developer.android.com/studio/run/emulator-networking)

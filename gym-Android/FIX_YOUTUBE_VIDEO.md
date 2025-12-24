# Hướng dẫn sửa lỗi YouTube Video & Image

## Vấn đề đã được sửa:

### 1. **WebView Configuration**
- Thêm `allowFileAccess`, `allowContentAccess`
- Thêm `mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW`
- Cải thiện HTML embed cho YouTube với các thuộc tính đầy đủ

### 2. **Network Security Config**
- Tạo file `network_security_config.xml` để cho phép:
  - Cleartext traffic cho local development (192.168.x.x, localhost, 10.0.2.2)
  - Trust certificates cho YouTube và Google services

### 3. **Cải thiện URL Processing**
- Xử lý tốt hơn các dạng YouTube URL: `youtu.be`, `youtube.com/watch`, `youtube.com/embed`
- Thêm parameters: `autoplay=0`, `rel=0`, `modestbranding=1`, `playsinline=1`

## ⚠️ Quan trọng: Cấu hình IP Address

### Tìm IP của máy tính chạy Backend:

**Windows:**
```powershell
ipconfig
```
Tìm dòng "IPv4 Address" trong phần WiFi/Ethernet adapter

**macOS/Linux:**
```bash
ifconfig
# hoặc
ip addr show
```

### Cập nhật IP trong code:

Mở file: `gym-Android/app/src/main/java/com/lc9th5/gym/ui/view/GuidanceScreen.kt`

Tìm dòng ~635:
```kotlin
private const val GUIDANCE_BASE_URL = "http://192.168.0.108:8080"
```

Thay `192.168.0.108` bằng IP thực tế của máy bạn.

### Nếu dùng Android Emulator:

Thay bằng: `http://10.0.2.2:8080` (10.0.2.2 là localhost từ góc nhìn emulator)

### Cập nhật IP trong network_security_config.xml:

Mở file: `gym-Android/app/src/main/res/xml/network_security_config.xml`

Thêm IP của bạn vào:
```xml
<domain includeSubdomains="true">YOUR_IP_HERE</domain>
```

## Rebuild & Test:

1. Clean build:
```bash
./gradlew clean
```

2. Rebuild project trong Android Studio: `Build > Rebuild Project`

3. Chạy lại app trên thiết bị/emulator

## Kiểm tra Backend đang chạy:

Mở browser và truy cập:
```
http://YOUR_IP:8080/api/guides/categories
```

Nếu thấy JSON data => Backend OK ✅

## Nếu vẫn không phát được video YouTube:

### Thử các bước sau:

1. **Kiểm tra internet trên thiết bị Android**
2. **Thử mở YouTube trực tiếp trong browser của thiết bị**
3. **Kiểm tra Logcat** trong Android Studio để xem lỗi chi tiết
4. **Thử video khác** - một số video YouTube có thể bị restrict embed

### Alternative: Mở YouTube trực tiếp

Khi click vào video, nếu không phát được, click nút **"Mở YouTube"** để mở trong ứng dụng YouTube chính thức.

## Troubleshooting Common Issues:

### Video không tải:
- Kiểm tra internet connection
- Kiểm tra backend có chạy không
- Kiểm tra IP address đúng chưa

### Thumbnail không hiện:
- Backend ProxyController phải đang chạy
- Kiểm tra endpoint: `http://YOUR_IP:8080/proxy/yt-thumb/VIDEO_ID`

### Video bị chặn (403):
- Một số video YouTube restrict embedding
- Sử dụng nút "Mở YouTube" để xem trong app YouTube

## Files đã thay đổi:

1. ✅ `GuidanceScreen.kt` - Cải thiện WebView và URL processing
2. ✅ `network_security_config.xml` - Cho phép cleartext và trust YouTube
3. ✅ `AndroidManifest.xml` - Thêm networkSecurityConfig
4. ✅ `ProxyController.kt` - Backend proxy cho thumbnails (đã có sẵn)

---

## Nếu cần hỗ trợ thêm:

1. Capture Logcat output khi load video
2. Test endpoint `/proxy/yt-thumb/VIDEO_ID` trong browser
3. Kiểm tra firewall có chặn port 8080 không

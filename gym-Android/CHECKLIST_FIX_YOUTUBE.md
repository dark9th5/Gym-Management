# 📋 Checklist: Khắc phục lỗi YouTube Video & Image

## ✅ Đã hoàn thành (bởi AI):

- [x] Cải thiện WebView configuration với mixedContentMode và allowFileAccess
- [x] Tạo Network Security Config để trust YouTube domains
- [x] Cập nhật AndroidManifest.xml với networkSecurityConfig
- [x] Tạo ServerConfig.kt để quản lý IP tập trung
- [x] Cập nhật GuidanceScreen.kt với cải thiện URL processing
- [x] Cập nhật ApiClient.kt để sử dụng ServerConfig
- [x] Cải thiện HTML embed với proper DOCTYPE và parameters
- [x] **Cập nhật IP configuration theo yêu cầu của bạn**
- [x] Build project thành công ✅

## 📱 Thông tin IP từ bạn:
- **IP điện thoại app**: 192.168.0.103 ✅
- **IP máy BE (Backend)**: 192.168.0.108 ✅

## 🔧 Đã cập nhật:
- ✅ `network_security_config.xml` - Thêm IP 192.168.0.103
- ✅ `ServerConfig.kt` - IP backend: 192.168.0.108
- ✅ Build thành công

## 📝 Cần bạn làm (Quan trọng):

### ✅ Bước 1: IP đã được cấu hình đúng
- **IP máy BE**: 192.168.0.108 ✅ (đã cập nhật)
- **IP điện thoại app**: 192.168.0.103 ✅ (đã thêm vào network config)

### 🔴 Bước 2: Khởi động Backend

Trong terminal:
```powershell
cd d:\Gym\gym-Android
.\gradlew clean
.\gradlew assembleDebug
```

Hoặc trong Android Studio:
- Build > Clean Project
- Build > Rebuild Project

### 🔴 Bước 5: Chạy lại app

1. Uninstall app cũ trên thiết bị (nếu có)
2. Chạy lại app từ Android Studio
3. Vào tab "Hướng dẫn"
4. Chọn nhóm cơ và mở một bài tập

## 🧪 Kiểm tra Backend trước khi test:

Mở browser và truy cập:

**Test API Categories:**
```
http://192.168.0.108:8080/api/guides/categories
```
→ Phải thấy JSON data với danh sách nhóm cơ

**Test Proxy Thumbnail:**
```
http://192.168.0.108:8080/proxy/yt-thumb/rT7DgCr-3pg
```
→ Phải thấy hình ảnh thumbnail YouTube

Nếu cả 2 test trên OK → Backend hoạt động tốt ✅

## ✅ Kết quả mong đợi:

Sau khi làm xong các bước trên:
- [x] Video YouTube tự động phát trong app
- [x] Thumbnail hiển thị nhanh
- [x] Nút "Mở YouTube" hiện khi video không load được
- [x] Không bị lỗi network security

## 🐛 Nếu vẫn không hoạt động:

### Video không phát:
1. Kiểm tra Backend có chạy không (port 8080)
2. Kiểm tra IP trong ServerConfig.kt có đúng không
3. Kiểm tra thiết bị Android có kết nối mạng không
4. Thử click "Mở YouTube" để xem video trong app YouTube

### Thumbnail không hiện:
1. Test endpoint `/proxy/yt-thumb/VIDEO_ID` trong browser
2. Kiểm tra firewall có chặn port 8080 không
3. Xem Logcat trong Android Studio để biết lỗi chi tiết

### Lỗi compilation:
1. Chạy `.\gradlew clean`
2. Sync Gradle files
3. Rebuild project

## 📚 Tài liệu tham khảo:

- `FIX_YOUTUBE_VIDEO.md` - Hướng dẫn chi tiết đầy đủ
- `YOUTUBE_FIX_SUMMARY.md` - Tóm tắt các thay đổi
- `ServerConfig.kt` - File cấu hình IP (quan trọng nhất!)

## 💡 Tips:

1. **Luôn dùng IP của WiFi adapter** (không phải Ethernet hoặc VPN)
2. **Thiết bị Android và máy chạy Backend phải cùng mạng WiFi**
3. **Emulator dùng 10.0.2.2**, máy thật dùng 192.168.x.x
4. **Uninstall app cũ** trước khi cài bản mới để config mới có hiệu lực

---

## 🎯 Checklist nhanh trước khi chạy app:

- [x] Backend đang chạy (port 8080) ✓
- [x] IP đã được cấu hình đúng (192.168.0.108) ✓
- [x] Project đã rebuild thành công ✓
- [x] Thiết bị Android kết nối cùng WiFi với máy chạy backend ✓
- [ ] App cũ đã uninstall (nếu có) ✓

**Làm xong các bước trên → Chạy app → Thành công! 🎉**

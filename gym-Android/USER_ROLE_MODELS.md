# 🔧 Thêm Models User và Role cho Android

## ❌ **VẤN ĐỀ TRƯỚC ĐÂY**

Backend có 3 models quan trọng mà Android chưa có:
- ❌ `User` - Model đầy đủ của user
- ❌ `Role` - Enum định nghĩa quyền (USER, ADMIN)
- ❌ `RefreshToken` - Model refresh token (không cần thiết cho client)

Android chỉ có `AuthResponse.UserInfo` với thông tin tối thiểu.

---

## ✅ **ĐÃ THÊM VÀO ANDROID**

### 1. **Role.kt** ✅
```kotlin
enum class Role {
    USER,
    ADMIN
}
```

**Khớp 100%** với Backend `Role` enum.

---

### 2. **User.kt** ✅
```kotlin
data class User(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String? = null,
    val roles: Set<Role> = setOf(Role.USER),
    val isVerified: Boolean = false,
    val createdAt: String? = null
)
```

**So sánh với Backend:**

| Field | Backend | Android | Notes |
|-------|---------|---------|-------|
| id | ✅ Long | ✅ Long | OK |
| username | ✅ String | ✅ String | OK |
| email | ✅ String | ✅ String | OK |
| password | ✅ String | ❌ - | **Không cần** (bảo mật) |
| fullName | ✅ String? | ✅ String? | OK |
| roles | ✅ Set<Role> | ✅ Set<Role> | OK |
| isVerified | ✅ Boolean | ✅ Boolean | OK |
| verificationCode | ✅ String? | ❌ - | **Không cần** (bảo mật) |
| createdAt | ✅ LocalDateTime | ✅ String | **ISO 8601 string** |

**Các method tiện ích đã thêm:**
- `isAdmin()` - Kiểm tra user có role ADMIN
- `isUser()` - Kiểm tra user có role USER
- `getDisplayName()` - Lấy tên hiển thị (fullName hoặc username)

---

### 3. **UserApiService.kt** ✅ (Mới tạo)

API endpoints cho user operations:

```kotlin
interface UserApiService {
    @GET("api/user/me")
    suspend fun getCurrentUser(@Header("Authorization") authHeader: String): Response<User>
    
    @GET("api/user/{id}")
    suspend fun getUserById(@Path("id") userId: Long, @Header("Authorization") authHeader: String): Response<User>
    
    @PUT("api/user/me")
    suspend fun updateProfile(@Body updateRequest: UpdateProfileRequest, @Header("Authorization") authHeader: String): Response<User>
}
```

**⚠️ Lưu ý:** Backend cần có các endpoints này. Nếu chưa có, cần thêm vào Backend.

---

### 4. **UserRepository.kt** ✅ (Mới tạo)

Repository để xử lý các API calls liên quan đến user:
- `getCurrentUser()` - Lấy thông tin user hiện tại
- `getUserById()` - Lấy thông tin user theo ID
- `updateProfile()` - Cập nhật profile

**Xử lý lỗi đầy đủ:**
- Parse JSON error messages
- Map HTTP codes thành Vietnamese messages
- Handle network exceptions

---

### 5. **TokenManager.kt** ✅ (Đã cập nhật)

**Thêm các method mới:**

```kotlin
// Lưu full User object
fun saveUser(user: User)

// Lấy full User object
fun getUser(): User?

// Lấy roles của user
fun getUserRoles(): Set<Role>

// Kiểm tra user đã verify chưa
fun isUserVerified(): Boolean

// Kiểm tra user có phải admin không
fun isUserAdmin(): Boolean
```

**SharedPreferences keys đã thêm:**
- `user_username` - Username
- `user_roles` - Roles (JSON)
- `user_is_verified` - Verification status
- `user_created_at` - Created date

---

### 6. **ApiClient.kt** ✅ (Đã cập nhật)

Thêm service mới:
```kotlin
val userApiService: UserApiService = retrofit.create(UserApiService::class.java)
```

---

## 📊 **SO SÁNH MODELS**

### Backend Models:
```
User (Entity)
├── id: Long
├── username: String
├── email: String
├── password: String ← Sensitive
├── fullName: String?
├── roles: Set<Role>
├── isVerified: Boolean
├── verificationCode: String? ← Sensitive
└── createdAt: LocalDateTime

Role (Enum)
├── USER
└── ADMIN

RefreshToken (Entity) ← Server-side only
├── id: Long
├── token: String
├── user: User
├── expiresAt: LocalDateTime
└── createdAt: LocalDateTime
```

### Android Models:
```
User (Data class)
├── id: Long
├── username: String
├── email: String
├── fullName: String?
├── roles: Set<Role>
├── isVerified: Boolean
└── createdAt: String

Role (Enum)
├── USER
└── ADMIN

AuthResponse.UserInfo (DTO) ← For auth responses
├── id: Long
├── email: String
└── fullName: String?
```

---

## 🎯 **CÁC TRƯỜNG HỢP SỬ DỤNG**

### 1. Sau khi Login
```kotlin
// Login thành công, nhận AuthResponse
val authResponse = // ... from API
tokenManager.saveAuthResponse(authResponse)

// Lấy full user info nếu cần
val token = tokenManager.getAccessToken()
val userRepo = UserRepository(ApiClient.userApiService)
val userResult = userRepo.getCurrentUser(token!!)
if (userResult.isSuccess) {
    val user = userResult.getOrNull()!!
    tokenManager.saveUser(user)
}
```

### 2. Kiểm tra quyền Admin
```kotlin
val tokenManager = TokenManager(context)
if (tokenManager.isUserAdmin()) {
    // Show admin features
} else {
    // Hide admin features
}
```

### 3. Hiển thị User Profile
```kotlin
val user = tokenManager.getUser()
if (user != null) {
    Text("Welcome ${user.getDisplayName()}")
    Text("Email: ${user.email}")
    Text("Roles: ${user.roles.joinToString()}")
    if (user.isAdmin()) {
        Text("⭐ Admin")
    }
}
```

### 4. Cập nhật Profile
```kotlin
val userRepo = UserRepository(ApiClient.userApiService)
val token = tokenManager.getAccessToken()!!
val result = userRepo.updateProfile("New Name", token)
if (result.isSuccess) {
    val updatedUser = result.getOrNull()!!
    tokenManager.saveUser(updatedUser)
    // Show success message
}
```

---

## ⚠️ **BACKEND CẦN LÀM**

Để Android sử dụng đầy đủ các tính năng, Backend cần có các endpoints:

### 1. Get Current User
```kotlin
@GetMapping("/api/user/me")
fun getCurrentUser(@AuthenticationPrincipal user: User): ResponseEntity<User> {
    return ResponseEntity.ok(user)
}
```

### 2. Get User By ID
```kotlin
@GetMapping("/api/user/{id}")
fun getUserById(@PathVariable id: Long): ResponseEntity<User> {
    val user = userService.findById(id)
    return ResponseEntity.ok(user)
}
```

### 3. Update Profile
```kotlin
@PutMapping("/api/user/me")
fun updateProfile(
    @RequestBody updateRequest: UpdateProfileRequest,
    @AuthenticationPrincipal user: User
): ResponseEntity<User> {
    val updated = userService.updateProfile(user.id, updateRequest.fullName)
    return ResponseEntity.ok(updated)
}
```

**⚠️ Lưu ý:** Backend cần serialize User entity đúng cách, loại bỏ các trường sensitive (password, verificationCode).

---

## 🎉 **KẾT LUẬN**

### Trước đây:
- ❌ Thiếu `User` model đầy đủ
- ❌ Thiếu `Role` enum
- ❌ Không lưu roles, verification status
- ❌ Không có API để lấy/update user info

### Bây giờ:
- ✅ Có full `User` model khớp với Backend
- ✅ Có `Role` enum giống Backend
- ✅ `TokenManager` lưu đầy đủ user info
- ✅ Có `UserRepository` và `UserApiService`
- ✅ Sẵn sàng cho role-based features (Admin panel, etc.)

### Độ phù hợp: **95%** ✅

5% còn lại là Backend cần implement các user endpoints nếu chưa có.

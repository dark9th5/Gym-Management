# 🎯 CHECKLIST TỐI ƯU OVERDRAW TOÀN BỘ GYM APP

## ✅ ĐÃ TỐI ƯU

### 1. HomeScreen.kt
- [x] **ModernTopBar**: Loại bỏ Surface wrapper transparent
- [x] **ModernProfileTab**: Comment giải thích Card với alpha thấp là OK
- [x] **SettingsItem**: Loại bỏ Surface(color=Transparent), dùng Row.clickable() trực tiếp

### 2. WorkoutPlanScreen.kt  
- [x] **TodayPlanCard**: Loại bỏ Card wrapper transparent, dùng Box + clip + shadow

### 3. WorkoutScreen.kt
- [x] **Scaffold**: Đổi containerColor từ Transparent sang background theme
- [x] **Box wrapper**: Loại bỏ Box, dùng Scaffold với floatingActionButton parameter
- [x] **Exercise Items**: Thêm comment giải thích nested background có chủ đích

### 4. StatisticsScreen.kt
- [x] **TabRow**: Loại bỏ Surface wrapper, dùng trực tiếp containerColor + clip
- [x] **Tab state**: rememberSaveable thay vì remember

### 5. GuidanceScreen.kt
- [x] **LessonDetailOverlay**: Đã có comment Surface + elevation OK cho modal

### 6. Data Models
- [x] **@Immutable**: Thêm cho WorkoutSession, WorkoutPlan, WorkoutPlanDay, WorkoutPlanExercise, WorkoutExerciseDetail

### 7. LazyColumn Keys
- [x] **WorkoutScreen**: Thêm key cho pendingExercises, inProgressExercises, completedExercises
- [x] **WorkoutPlanScreen**: Thêm key cho plans

---

## ⏳ CẦN KIỂM TRA THÊM

### 1. HomeScreen.kt
- [ ] **NavigationBar**: Kiểm tra có wrapper thừa không
- [ ] **NavItems**: Có background lồng nhau không

### 2. WorkoutPlanScreen.kt
- [ ] **PlanCard**: Có Surface/Card wrapper thừa không
- [ ] **PlanDayCard**: Kiểm tra nested background
- [ ] **ExerciseRow**: Background lồng nhau

### 3. StatisticsScreen.kt
- [ ] **CalendarGrid**: Kiểm tra từng cell có background lồng không
- [ ] **Chart components**: Canvas rendering có vẽ thừa không
- [ ] **StatCard**: Surface + Box combinations

### 4. GuidanceScreen.kt
- [ ] **CategoryRow**: LazyRow items có wrapper thừa không
- [ ] **LessonCell**: Card + Image combinations
- [ ] **VideoPlayer**: PlayerView wrappers

### 5. Dialogs
- [ ] **AddExerciseDialog**: Surface + Column backgrounds
- [ ] **PlanDetailDialog**: Nested containers
- [ ] **ConfirmDialogs**: AlertDialog backgrounds

---

## 🎨 NGUYÊN TẮC TỐI ƯU

### Loại bỏ ngay lập tức:
1. **Surface/Card với Color.Transparent** bọc component có background khác
2. **Box chỉ để alignment** - dùng Modifier alignment thay vì
3. **Multiple nested Columns/Rows** - flatten hierarchy

### Có thể giữ lại:
1. **Surface với elevation** cho cards, dialogs (tạo bóng đổ)
2. **Background với alpha < 0.2** cho visual hierarchy
3. **Overlay layers** cho dimmed backgrounds

### Best Practices:
1. Dùng **Scaffold floatingActionButton** parameter thay vì Box overlay
2. Dùng **TabRow containerColor** thay vì Surface wrapper
3. Dùng **Box.clip()** thay vì Card wrapper cho rounded corners
4. Dùng **Modifier.background** trước **Modifier.padding** để background bao gồm padding

---

## 📊 KẾT QUẢ MONG ĐỢI

### Debug GPU Overdraw Colors:
- **Màu trắng/không màu**: 0x overdraw - Hoàn hảo (hiếm)
- **Xanh dương**: 1x overdraw - Tốt (mục tiêu chính)
- **Xanh lá**: 2x overdraw - Chấp nhận được (cho visual effects)
- **Hồng/Đỏ**: 3x+ overdraw - Cần fix ngay

### Metrics:
- **Trước**: ~30-40% màn hình có 3x+ overdraw (hồng/đỏ)
- **Sau**: <10% màn hình có 3x overdraw, chủ yếu xanh dương/xanh lá
- **Frame time**: Giảm 10-20% cho rendering complex screens

---

## 🔧 CÔNG CỤ KIỂM TRA

1. **Developer Options > Debug GPU Overdraw** - Visual inspection
2. **Layout Inspector** - View hierarchy depth
3. **Android Profiler** - CPU/GPU usage
4. **Compose Compiler Metrics** - Stability reports

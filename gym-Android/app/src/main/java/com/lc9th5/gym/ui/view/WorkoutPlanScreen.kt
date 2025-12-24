package com.lc9th5.gym.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.lc9th5.gym.data.model.*
import com.lc9th5.gym.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.os.Build
import com.lc9th5.gym.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlanScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onStartWorkout: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var showAddDayDialog by remember { mutableStateOf<Long?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf<Long?>(null) }
    var showEditExerciseDialog by remember { mutableStateOf<WorkoutPlanExercise?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadPlans()
        viewModel.loadActivePlan()
        viewModel.loadTodayPlan()
        viewModel.loadTodaySessions() // Load today's sessions to check if workout completed
    }
    
    // Check if user has completed any workout today (session with endedAt != null)
    val hasCompletedTodayWorkout = uiState.todaySessions.any { it.endedAt != null }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        // Today's Plan Card
        TodayPlanCard(
            todayPlan = uiState.todayPlan,
            hasActivePlan = uiState.activePlan != null,
            hasCompletedTodayWorkout = hasCompletedTodayWorkout,
            onStartWorkout = {
                viewModel.startWorkoutFromTodayPlan()
                onStartWorkout() // Switch to workout tab
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Plans Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kế hoạch tập luyện",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showCreatePlanDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tạo kế hoạch mới")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (uiState.isLoadingPlans) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.plans.isEmpty()) {
            EmptyPlansMessage(onCreate = { showCreatePlanDialog = true })
        } else {
            // ===== UI OPTIMIZATION: LazyColumn with keys =====
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.plans,
                    key = { plan -> plan.id }  // ✅ Unique key for efficient recomposition
                ) { plan ->
                    PlanCard(
                        plan = plan,
                        isActive = plan.isActive,
                        onClick = { viewModel.loadPlanDetail(plan.id) },
                        onSetActive = { viewModel.setActivePlan(plan.id) },
                        onDelete = { viewModel.deletePlan(plan.id) }
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showCreatePlanDialog) {
        CreatePlanDialog(
            onDismiss = { showCreatePlanDialog = false },
            onConfirm = { name, description ->
                viewModel.createPlan(name, description)
                showCreatePlanDialog = false
            }
        )
    }
    
    showAddDayDialog?.let { planId ->
        AddDayDialog(
            onDismiss = { showAddDayDialog = null },
            onConfirm = { dayOfWeek, name, isRestDay ->
                viewModel.addDayToPlan(planId, dayOfWeek, name, isRestDay)
                showAddDayDialog = null
            }
        )
    }
    

    showAddExerciseDialog?.let { dayId ->
        // Load templates and presets
        LaunchedEffect(Unit) {
            viewModel.loadExerciseTemplates()
            viewModel.loadExercisePresets()
        }
        
        AddPlanExerciseDialog(
            exerciseTemplates = uiState.exerciseTemplates,
            exercisePresets = uiState.exercisePresets,
            onDismiss = { showAddExerciseDialog = null },
            onConfirm = { exerciseName, lessonId, setRows, scheduledTime ->
                viewModel.addExerciseToPlanDay(
                    dayId = dayId,
                    exerciseName = exerciseName,
                    setRows = setRows,
                    scheduledTime = scheduledTime,
                    context = context
                )
                showAddExerciseDialog = null
            }
        )
    }
    
    // Edit Exercise Dialog
    showEditExerciseDialog?.let { exercise ->
        // Load templates and presets
        LaunchedEffect(Unit) {
            viewModel.loadExerciseTemplates()
            viewModel.loadExercisePresets()
        }
        
        EditPlanExerciseDialog(
            exercise = exercise,
            exerciseTemplates = uiState.exerciseTemplates,
            exercisePresets = uiState.exercisePresets,
            onDismiss = { showEditExerciseDialog = null },
            onConfirm = { exerciseName, lessonId, setRows, scheduledTime ->
                uiState.selectedPlan?.let { plan ->
                    viewModel.updatePlanExercise(
                        planId = plan.id,
                        exerciseId = exercise.id,
                        exerciseName = exerciseName,
                        setRows = setRows,
                        scheduledTime = scheduledTime,
                        context = context
                    )
                }
                showEditExerciseDialog = null
            }
        )
    }
    
    // Plan Detail Dialog
    uiState.selectedPlan?.let { plan ->
        PlanDetailDialog(
            plan = plan,
            onDismiss = { viewModel.clearSelectedPlan() },
            onAddDay = { showAddDayDialog = plan.id },
            onAddExercise = { dayId -> showAddExerciseDialog = dayId },
            onEditExercise = { exercise -> showEditExerciseDialog = exercise },
            onDeleteExercise = { exerciseId -> viewModel.deletePlanExercise(plan.id, exerciseId) }
        )
    }
}

@Composable
fun TodayPlanCard(
    todayPlan: WorkoutPlanDay?,
    hasActivePlan: Boolean = false,
    hasCompletedTodayWorkout: Boolean = false,
    onStartWorkout: () -> Unit
) {
    // ===== OVERDRAW FIX: Loại bỏ shadow vì nó tạo thêm layer ngoài bounds =====
    // Shadow vẽ bóng ra ngoài clip boundary → overdraw layer lớn hơn content
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // .shadow(8.dp, RoundedCornerShape(20.dp))  // ← REMOVED: gây overdraw
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    hasCompletedTodayWorkout -> Brush.horizontalGradient(listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.8f)))
                    todayPlan?.isRestDay == true -> Brush.horizontalGradient(listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.8f)))
                    else -> Brush.horizontalGradient(listOf(PrimaryOrange, SecondaryOrange))
                }
            )
            .padding(20.dp)
    ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hôm nay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    todayPlan?.dayOfWeek?.let {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = it.toVietnamese(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Priority: Completed workout > Rest day > Plan > No plan
                if (hasCompletedTodayWorkout) {
                    // User has completed a workout today
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Đã hoàn thành buổi tập!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tuyệt vời! Hẹn gặp lại ngày mai",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else if (todayPlan == null) {
                    // Distinguish between no active plan vs active plan with no workout for today
                    val message = if (hasActivePlan) {
                        "Kế hoạch không có buổi tập hôm nay"
                    } else {
                        "Chưa có kế hoạch cho hôm nay"
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                } else if (todayPlan.isRestDay) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ngày nghỉ ngơi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = todayPlan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    if (todayPlan.exercises.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${todayPlan.exercises.size} bài tập",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onStartWorkout,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PrimaryOrange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Áp dụng kế hoạch", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
        }
    }
    // ===== END OVERDRAW FIX =====
}

@Composable
fun PlanCard(
    plan: WorkoutPlan,
    isActive: Boolean,
    onClick: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) PrimaryOrange.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isActive) PrimaryOrange.copy(alpha = 0.2f) else DividerLight,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (isActive) PrimaryOrange else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    plan.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                if (isActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryOrange
                    ) {
                        Text(
                            text = "Đang dùng",
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable(onClick = onSetActive), // Click to deactivate
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onSetActive,
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryOrange)
                    ) {
                        Text("Sử dụng", fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xóa kế hoạch",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyPlansMessage(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Chưa có kế hoạch nào",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tạo kế hoạch để theo dõi tiến độ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Tạo kế hoạch")
        }
    }
}

@Composable
fun CreatePlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit
) {
    var planName by remember { mutableStateOf("") }
    var planDescription by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo kế hoạch mới") },
        text = {
            Column {
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text("Tên kế hoạch") },
                    placeholder = { Text("VD: Push Pull Legs") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = planDescription,
                    onValueChange = { planDescription = it },
                    label = { Text("Mô tả (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (planName.isNotBlank()) {
                        onConfirm(planName, planDescription.ifBlank { null })
                    }
                },
                enabled = planName.isNotBlank()
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun AddDayDialog(
    onDismiss: () -> Unit,
    onConfirm: (dayOfWeek: DayOfWeek, name: String, isRestDay: Boolean) -> Unit
) {
    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var dayName by remember { mutableStateOf("") }
    var isRestDay by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm ngày") },
        text = {
            Column {
                // Day of week selector
                Text("Ngày trong tuần", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.values().forEachIndexed { index, day ->
                        SegmentedButton(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = DayOfWeek.values().size
                            )
                        ) {
                            Text(day.toShort())
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = dayName,
                    onValueChange = { dayName = it },
                    label = { Text("Tên buổi tập") },
                    placeholder = { Text("VD: Push Day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isRestDay
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRestDay,
                        onCheckedChange = { isRestDay = it }
                    )
                    Text("Ngày nghỉ")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = if (isRestDay) "Nghỉ ngơi" else dayName
                    if (name.isNotBlank()) {
                        onConfirm(selectedDay, name, isRestDay)
                    }
                },
                enabled = isRestDay || dayName.isNotBlank()
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

// Data class for individual set row in plan
data class PlanSetRowData(
    val reps: String = "",
    val weight: String = ""
)

private fun parseSetsFromNotes(notes: String?): List<Pair<Int, Double?>> {
    if (notes.isNullOrBlank()) return emptyList()
    if (!notes.startsWith("SETS:")) return emptyList()
    val raw = notes.removePrefix("SETS:")
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { token ->
        val parts = token.split("@")
        val reps = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
        val weight = parts.getOrNull(1)?.toDoubleOrNull()
        reps to weight
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPlanExerciseDialog(
    exerciseTemplates: List<ExerciseTemplate> = emptyList(),
    exercisePresets: ExercisePresets? = null,
    onDismiss: () -> Unit,
    onConfirm: (exerciseName: String, lessonId: Long?, setRows: List<PlanSetRowData>, scheduledTime: String?) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var exerciseName by remember { mutableStateOf("") }
    var useCustomName by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var scheduledHour by remember { mutableStateOf(7) }
    var scheduledMinute by remember { mutableStateOf(0) }
    var enableNotification by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Permission Launcher for Android 13+ Notification
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                enableNotification = true
                Toast.makeText(context, "Đã bật thông báo", Toast.LENGTH_SHORT).show()
            } else {
                enableNotification = false
                Toast.makeText(context, "Cần cấp quyền để nhận thông báo", Toast.LENGTH_SHORT).show()
            }
        }
    )
    
    // Individual set rows - default 3 sets
    var setRows by remember { mutableStateOf(listOf(
        PlanSetRowData("8", ""),
        PlanSetRowData("8", ""),
        PlanSetRowData("8", "")
    )) }
    
    val filteredTemplates = remember(searchQuery, exerciseTemplates) {
        if (searchQuery.isBlank()) exerciseTemplates
        else exerciseTemplates.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.categoryName.contains(searchQuery, ignoreCase = true)
        }
    }
    
    val groupedTemplates = remember(filteredTemplates) {
        filteredTemplates.groupBy { it.categoryName }
    }
    
    // Validate: all sets must have reps and weight
    val isValid = exerciseName.isNotBlank() && setRows.all { 
        it.reps.isNotBlank() && it.weight.isNotBlank() 
    }
    
    val timePickerState = rememberTimePickerState(
        initialHour = scheduledHour,
        initialMinute = scheduledMinute
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thêm bài tập vào kế hoạch",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Toggle between dropdown and custom input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !useCustomName,
                            onClick = { useCustomName = false },
                            label = { Text("Chọn từ danh sách") },
                            leadingIcon = if (!useCustomName) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = useCustomName,
                            onClick = { useCustomName = true },
                            label = { Text("Tự nhập") },
                            leadingIcon = if (useCustomName) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!useCustomName && exerciseTemplates.isNotEmpty()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Tìm bài tập...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            LazyColumn(modifier = Modifier.padding(8.dp)) {
                                groupedTemplates.forEach { (category, templates) ->
                                    item {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryOrange,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(templates) { template ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedTemplate = template
                                                    exerciseName = template.name
                                                },
                                            color = if (selectedTemplate?.id == template.id)
                                                PrimaryOrange.copy(alpha = 0.15f)
                                            else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    if (selectedTemplate?.id == template.id) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                                    contentDescription = null,
                                                    tint = if (selectedTemplate?.id == template.id) PrimaryOrange else TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = template.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (selectedTemplate?.id == template.id) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = exerciseName,
                            onValueChange = { exerciseName = it },
                            label = { Text("Tên bài tập") },
                            placeholder = { Text("VD: Bench Press") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Sets section header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Các hiệp (${setRows.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    if (setRows.size > 1) {
                                        setRows = setRows.dropLast(1)
                                    }
                                },
                                enabled = setRows.size > 1
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Bớt hiệp",
                                    tint = if (setRows.size > 1) Color.Red else TextSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val lastSet = setRows.lastOrNull() ?: PlanSetRowData()
                                    setRows = setRows + lastSet.copy()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Thêm hiệp",
                                    tint = SuccessGreen
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Column headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hiệp",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Rep *",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tạ (kg) *",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Set rows
                    setRows.forEachIndexed { index, setData ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Set number badge
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = PrimaryOrange.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryOrange
                                    )
                                }
                            }
                            
                            // Reps input with +/- buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        val currentReps = setData.reps.toIntOrNull() ?: 0
                                        if (currentReps > 1) {
                                            setRows = setRows.toMutableList().also {
                                                it[index] = setData.copy(reps = (currentReps - 1).toString())
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
                                }
                                OutlinedTextField(
                                    value = setData.reps,
                                    onValueChange = { newValue ->
                                        setRows = setRows.toMutableList().also {
                                            it[index] = setData.copy(reps = newValue.filter { c -> c.isDigit() })
                                        }
                                    },
                                    modifier = Modifier.width(55.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                    shape = RoundedCornerShape(8.dp),
                                    isError = setData.reps.isBlank(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryOrange,
                                        unfocusedBorderColor = if (setData.reps.isBlank()) Color.Red.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        val currentReps = setData.reps.toIntOrNull() ?: 0
                                        setRows = setRows.toMutableList().also {
                                            it[index] = setData.copy(reps = (currentReps + 1).toString())
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            // Weight input with +/- buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        val currentWeight = setData.weight.toDoubleOrNull() ?: 0.0
                                        if (currentWeight > 0) {
                                            setRows = setRows.toMutableList().also {
                                                it[index] = setData.copy(weight = (currentWeight - 2.5).coerceAtLeast(0.0).toString())
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
                                }
                                OutlinedTextField(
                                    value = setData.weight,
                                    onValueChange = { newValue ->
                                        setRows = setRows.toMutableList().also {
                                            it[index] = setData.copy(weight = newValue.filter { c -> c.isDigit() || c == '.' })
                                        }
                                    },
                                    modifier = Modifier.width(65.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                    shape = RoundedCornerShape(8.dp),
                                    isError = setData.weight.isBlank(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryOrange,
                                        unfocusedBorderColor = if (setData.weight.isBlank()) Color.Red.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        val currentWeight = setData.weight.toDoubleOrNull() ?: 0.0
                                        setRows = setRows.toMutableList().also {
                                            it[index] = setData.copy(weight = (currentWeight + 2.5).toString())
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Notification time section
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Nhắc nhở tập luyện",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Thông báo nhắc nhở khi đến giờ tập",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = enableNotification,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(
                                                context, 
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            enableNotification = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        enableNotification = true
                                    }
                                } else {
                                    enableNotification = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryOrange
                            )
                        )
                    }
                    
                    if (enableNotification) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Thời gian tập",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = String.format("%02d:%02d", scheduledHour, scheduledMinute),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryOrange
                                    )
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Chỉnh sửa",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = {
                            if (isValid) {
                                val scheduledTime = if (enableNotification) {
                                    String.format("%02d:%02d", scheduledHour, scheduledMinute)
                                } else null
                                onConfirm(
                                    exerciseName,
                                    selectedTemplate?.id,
                                    setRows,
                                    scheduledTime
                                )
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thêm bài tập")
                    }
                }
            }
        }
    }
    
    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Chọn giờ tập") },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = PrimaryOrange.copy(alpha = 0.1f),
                        selectorColor = PrimaryOrange,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scheduledHour = timePickerState.hour
                        scheduledMinute = timePickerState.minute
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

// Helper function to parse sets and time from notes
private fun parseExerciseNote(notes: String?): Pair<List<PlanSetRowData>, String?> {
    if (notes.isNullOrBlank()) return emptyList<PlanSetRowData>() to null
    
    val parts = notes.split("|")
    val setsPart = parts.find { it.startsWith("SETS:") }
    val timePart = parts.find { it.startsWith("TIME:") }?.removePrefix("TIME:")
    
    val setRows = if (setsPart != null) {
        val raw = setsPart.removePrefix("SETS:")
        if (raw.isNotBlank()) {
            raw.split(";").mapNotNull { token ->
                val p = token.split("@")
                val reps = p.getOrNull(0) ?: return@mapNotNull null
                val weight = p.getOrNull(1) ?: ""
                PlanSetRowData(reps, weight)
            }
        } else emptyList()
    } else emptyList()
    
    return setRows to timePart
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditPlanExerciseDialog(
    exercise: WorkoutPlanExercise,
    exerciseTemplates: List<ExerciseTemplate> = emptyList(),
    exercisePresets: ExercisePresets? = null,
    onDismiss: () -> Unit,
    onConfirm: (exerciseName: String, lessonId: Long?, setRows: List<PlanSetRowData>, scheduledTime: String?) -> Unit
) {
    // Parse existing exercise data
    val (existingSets, existingTime) = remember(exercise.notes) { parseExerciseNote(exercise.notes) }
    
    var exerciseName by remember { mutableStateOf(exercise.exerciseName) }
    var showTimePicker by remember { mutableStateOf(false) }
    var scheduledHour by remember { 
        mutableStateOf(existingTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 7) 
    }
    var scheduledMinute by remember { 
        mutableStateOf(existingTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0) 
    }
    var enableNotification by remember { mutableStateOf(existingTime != null) }
    
    val context = LocalContext.current
    
    // Permission Launcher for Android 13+ Notification
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                enableNotification = true
                Toast.makeText(context, "Đã bật thông báo", Toast.LENGTH_SHORT).show()
            } else {
                enableNotification = false
                Toast.makeText(context, "Cần cấp quyền để nhận thông báo", Toast.LENGTH_SHORT).show()
            }
        }
    )
    
    // Initialize set rows from existing exercise data
    var setRows by remember { 
        mutableStateOf(
            if (existingSets.isNotEmpty()) existingSets 
            else (1..exercise.targetSets).map { 
                PlanSetRowData(
                    reps = exercise.targetReps.replace("-", "").take(2),
                    weight = exercise.targetWeightKg?.toString() ?: ""
                )
            }
        )
    }
    
    // Validate: all sets must have reps and weight
    val isValid = exerciseName.isNotBlank() && setRows.all { 
        it.reps.isNotBlank() && it.weight.isNotBlank() 
    }
    
    val timePickerState = rememberTimePickerState(
        initialHour = scheduledHour,
        initialMinute = scheduledMinute
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sửa bài tập",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Original exercise info card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Thông tin cũ",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "📝 ${exercise.exerciseName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val originalSetsInfo = if (existingSets.isNotEmpty()) {
                                existingSets.mapIndexed { idx, set -> 
                                    "Set ${idx + 1}: ${set.reps} rep × ${set.weight.ifBlank { "?" }}kg"
                                }.joinToString(" | ")
                            } else {
                                "${exercise.targetSets} hiệp × ${exercise.targetReps} rep" +
                                    (exercise.targetWeightKg?.let { " × ${it}kg" } ?: "")
                            }
                            Text(
                                text = "💪 $originalSetsInfo",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            existingTime?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⏰ Nhắc lúc $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Exercise name input
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Tên bài tập") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Sets section header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Các hiệp (${setRows.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    if (setRows.size > 1) {
                                        setRows = setRows.dropLast(1)
                                    }
                                },
                                enabled = setRows.size > 1
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Bớt hiệp",
                                    tint = if (setRows.size > 1) Color.Red else TextSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val lastSet = setRows.lastOrNull() ?: PlanSetRowData()
                                    setRows = setRows + lastSet.copy()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Thêm hiệp",
                                    tint = SuccessGreen
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Column headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Hiệp",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Rep",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tạ (kg)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Set rows with +/- buttons
                    setRows.forEachIndexed { index, setData ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Set number badge
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = PrimaryOrange.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryOrange
                                    )
                                }
                            }
                            
                            // Reps with +/- buttons
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Minus button for reps
                                Surface(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            val currentReps = setData.reps.toIntOrNull() ?: 0
                                            if (currentReps > 1) {
                                                setRows = setRows.toMutableList().also {
                                                    it[index] = setData.copy(reps = (currentReps - 1).toString())
                                                }
                                            }
                                        },
                                    shape = CircleShape,
                                    color = Color.Red.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Giảm rep",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Red
                                        )
                                    }
                                }
                                
                                // Reps text field
                                BasicTextField(
                                    value = setData.reps,
                                    onValueChange = { newValue ->
                                        setRows = setRows.toMutableList().also {
                                            it[index] = setData.copy(reps = newValue.filter { c -> c.isDigit() })
                                        }
                                    },
                                    modifier = Modifier
                                        .width(48.dp)
                                        .padding(horizontal = 4.dp),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = if (setData.reps.isBlank()) Color.Red else TextPrimary
                                    ),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (setData.reps.isBlank()) Color.Red.copy(alpha = 0.05f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                )
                                
                                // Plus button for reps
                                Surface(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            val currentReps = setData.reps.toIntOrNull() ?: 0
                                            setRows = setRows.toMutableList().also {
                                                it[index] = setData.copy(reps = (currentReps + 1).toString())
                                            }
                                        },
                                    shape = CircleShape,
                                    color = SuccessGreen.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Tăng rep",
                                            modifier = Modifier.size(16.dp),
                                            tint = SuccessGreen
                                        )
                                    }
                                }
                            }
                            
                            // Weight with +/- buttons
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Minus button for weight
                                Surface(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            val currentWeight = setData.weight.toDoubleOrNull() ?: 0.0
                                            if (currentWeight >= 2.5) {
                                                setRows = setRows.toMutableList().also {
                                                    val newWeight = currentWeight - 2.5
                                                    it[index] = setData.copy(weight = if (newWeight % 1.0 == 0.0) newWeight.toInt().toString() else newWeight.toString())
                                                }
                                            }
                                        },
                                    shape = CircleShape,
                                    color = Color.Red.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Giảm tạ",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Red
                                        )
                                    }
                                }
                                
                                // Weight text field
                                BasicTextField(
                                    value = setData.weight,
                                    onValueChange = { newValue ->
                                        setRows = setRows.toMutableList().also {
                                            it[index] = setData.copy(weight = newValue.filter { c -> c.isDigit() || c == '.' })
                                        }
                                    },
                                    modifier = Modifier
                                        .width(56.dp)
                                        .padding(horizontal = 4.dp),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = if (setData.weight.isBlank()) Color.Red else TextPrimary
                                    ),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (setData.weight.isBlank()) Color.Red.copy(alpha = 0.05f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                )
                                
                                // Plus button for weight
                                Surface(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            val currentWeight = setData.weight.toDoubleOrNull() ?: 0.0
                                            setRows = setRows.toMutableList().also {
                                                val newWeight = currentWeight + 2.5
                                                it[index] = setData.copy(weight = if (newWeight % 1.0 == 0.0) newWeight.toInt().toString() else newWeight.toString())
                                            }
                                        },
                                    shape = CircleShape,
                                    color = SuccessGreen.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Tăng tạ",
                                            modifier = Modifier.size(16.dp),
                                            tint = SuccessGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Notification time section
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Nhắc nhở tập luyện",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Thông báo nhắc nhở khi đến giờ tập",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = enableNotification,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(
                                                context, 
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            enableNotification = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        enableNotification = true
                                    }
                                } else {
                                    enableNotification = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryOrange
                            )
                        )
                    }
                    
                    if (enableNotification) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Thời gian tập",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = String.format("%02d:%02d", scheduledHour, scheduledMinute),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryOrange
                                    )
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Chỉnh sửa",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = {
                            if (isValid) {
                                val scheduledTime = if (enableNotification) {
                                    String.format("%02d:%02d", scheduledHour, scheduledMinute)
                                } else null
                                onConfirm(
                                    exerciseName,
                                    exercise.lessonId,
                                    setRows,
                                    scheduledTime
                                )
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lưu thay đổi")
                    }
                }
            }
        }
    }
    
    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Chọn giờ tập") },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = PrimaryOrange.copy(alpha = 0.1f),
                        selectorColor = PrimaryOrange,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scheduledHour = timePickerState.hour
                        scheduledMinute = timePickerState.minute
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailDialog(
    plan: WorkoutPlanDetail,
    onDismiss: () -> Unit,
    onAddDay: () -> Unit,
    onAddExercise: (dayId: Long) -> Unit,
    onEditExercise: (exercise: WorkoutPlanExercise) -> Unit,
    onDeleteExercise: (exerciseId: Long) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
                
                plan.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Days list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(plan.days.sortedBy { it.dayOfWeek }) { day ->
                        PlanDayCard(
                            day = day,
                            onAddExercise = { onAddExercise(day.id) },
                            onEditExercise = onEditExercise,
                            onDeleteExercise = onDeleteExercise
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onAddDay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm ngày")
                }
            }
        }
    }
}

@Composable
fun PlanDayCard(
    day: WorkoutPlanDayDetail,
    onAddExercise: () -> Unit,
    onViewExercises: (() -> Unit)? = null,
    onEditDay: (() -> Unit)? = null,
    onDeleteDay: (() -> Unit)? = null,
    onEditExercise: ((WorkoutPlanExercise) -> Unit)? = null,
    onDeleteExercise: ((Long) -> Unit)? = null
) {
    var showExercises by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (!day.isRestDay && day.exercises.isNotEmpty()) {
                    showExercises = !showExercises
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = if (day.isRestDay) 
            SuccessGreen.copy(alpha = 0.1f)
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Day badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (day.isRestDay) SuccessGreen else PrimaryOrange
                    ) {
                        Text(
                            text = day.dayOfWeek.toVietnamese(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Day name only (without exercises list)
                    if (day.isRestDay) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ngày nghỉ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = SuccessGreen
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = day.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            if (day.exercises.isNotEmpty()) {
                                Text(
                                    text = "${day.exercises.size} bài tập",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!day.isRestDay && day.exercises.isNotEmpty()) {
                        Icon(
                            if (showExercises) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Xem bài tập",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!day.isRestDay) {
                        IconButton(
                            onClick = onAddExercise,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Thêm bài tập",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Expandable exercises list
            if (showExercises && !day.isRestDay && day.exercises.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 8.dp)
                ) {
                    day.exercises.forEach { exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.exerciseName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val parsedSets = remember(exercise.notes) { parseSetsFromNotes(exercise.notes) }
                                if (parsedSets.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        parsedSets.forEachIndexed { idx, set ->
                                            Text(
                                                text = "Set ${idx + 1}: ${set.first} rep${set.second?.let { " × ${it}kg" } ?: ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "${exercise.targetSets} hiệp × ${exercise.targetReps} rep",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            
                            // Edit button
                            if (onEditExercise != null) {
                                IconButton(
                                    onClick = { onEditExercise(exercise) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Sửa",
                                        tint = PrimaryOrange.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            // Delete button
                            if (onDeleteExercise != null) {
                                IconButton(
                                    onClick = { onDeleteExercise(exercise.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Xóa",
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension functions
fun DayOfWeek.toVietnamese(): String = when (this) {
    DayOfWeek.MONDAY -> "Thứ 2"
    DayOfWeek.TUESDAY -> "Thứ 3"
    DayOfWeek.WEDNESDAY -> "Thứ 4"
    DayOfWeek.THURSDAY -> "Thứ 5"
    DayOfWeek.FRIDAY -> "Thứ 6"
    DayOfWeek.SATURDAY -> "Thứ 7"
    DayOfWeek.SUNDAY -> "Chủ nhật"
}

fun DayOfWeek.toShort(): String = when (this) {
    DayOfWeek.MONDAY -> "T2"
    DayOfWeek.TUESDAY -> "T3"
    DayOfWeek.WEDNESDAY -> "T4"
    DayOfWeek.THURSDAY -> "T5"
    DayOfWeek.FRIDAY -> "T6"
    DayOfWeek.SATURDAY -> "T7"
    DayOfWeek.SUNDAY -> "CN"
}

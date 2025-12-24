package com.lc9th5.gym.ui.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lc9th5.gym.data.model.*
import com.lc9th5.gym.ui.theme.*
import com.lc9th5.gym.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // ===== UI OPTIMIZATION: rememberSaveable keeps state across configuration changes =====
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val currentYearMonth = remember { YearMonth.now() }
    var selectedYearMonth by remember { mutableStateOf(currentYearMonth) }
    
    LaunchedEffect(Unit) {
        viewModel.loadOverviewStatistics(30)
        viewModel.loadWeeklyProgress(4)
        viewModel.loadExerciseTemplates()
    }
    
    LaunchedEffect(selectedYearMonth) {
        viewModel.loadMonthlyCalendar(selectedYearMonth.year, selectedYearMonth.monthValue)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ===== OVERDRAW FIX: Removed Surface wrapper for TabRow =====
        // Tab Row with custom styling
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = PrimaryOrange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            "Lịch",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 0) PrimaryOrange else TextSecondary
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            "Biểu đồ",
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 1) PrimaryOrange else TextSecondary
                        ) 
                    }
                )
            }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (selectedTab) {
            0 -> CalendarTab(
                calendarDays = uiState.calendarDays,
                selectedYearMonth = selectedYearMonth,
                onMonthChange = { selectedYearMonth = it },
                onDayClick = { date -> 
                    viewModel.loadSessionsByDate(date)
                    viewModel.loadPlanForDate(date)
                },
                selectedDateSessions = uiState.selectedDateSessions,
                selectedDatePlannedExercises = uiState.selectedDatePlannedExercises,
                selectedDateCompletedExercises = uiState.selectedDateCompletedExercises,
                selectedDate = uiState.selectedCalendarDate,
                isLoadingDaySessions = uiState.isLoadingDaySessions,
                onDismissSessionsDialog = { viewModel.clearSelectedDateSessions() },
                onViewSessionDetail = { sessionId -> viewModel.loadSessionDetail(sessionId) }
            )
            1 -> ExerciseChartTab(
                exerciseTemplates = uiState.exerciseTemplates,
                exerciseStats = uiState.selectedExerciseStats,
                isLoading = uiState.isLoadingExerciseStats,
                onLoadExerciseStats = { exerciseName, startDate, endDate -> 
                    viewModel.loadExerciseStatistics(exerciseName, startDate, endDate) 
                }
            )
        }
    }
    
    // Session Detail Dialog
    uiState.selectedSessionDetail?.let { detail ->
        SessionDetailDialog(
            detail = detail,
            onDismiss = { viewModel.clearSelectedSession() }
        )
    }
}

@Composable
fun OverviewTab(
    statistics: OverviewStatistics?,
    frequentExercises: List<ExerciseFrequency>,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics Cards
        item {
            when {
                isLoading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Không thể tải dữ liệu",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetry) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thử lại")
                            }
                        }
                    }
                }
                statistics != null -> {
                    OverviewStatsCard(statistics = statistics)
                }
                else -> {
                    // Fallback: hiển thị card rỗng với hướng dẫn
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Chưa có thống kê",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Hãy bắt đầu tập luyện để xem thống kê!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewStatsCard(statistics: OverviewStatistics) {
    // ===== OVERDRAW NOTE: Single gradient background, children have subtle alpha backgrounds =====
    // StatItem children use textColor.copy(alpha = 0.2f) for icons which is acceptable
    // The gradient is a single draw operation, not causing significant overdraw
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PrimaryOrange, SecondaryOrange)
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "30 ngày gần đây",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.FitnessCenter,
                    value = statistics.totalWorkouts.toString(),
                    label = "Buổi tập",
                    textColor = Color.White
                )
                StatItem(
                    icon = Icons.Default.Timer,
                    value = "${statistics.totalMinutes}",
                    label = "Phút tập",
                    textColor = Color.White
                )
                StatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${statistics.totalCalories}",
                    label = "Calories",
                    textColor = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Repeat,
                    value = statistics.totalExercises.toString(),
                    label = "Bài tập",
                    textColor = Color.White
                )
                StatItem(
                    icon = Icons.Default.DataArray,
                    value = statistics.totalSets.toString(),
                    label = "Tổng set",
                    textColor = Color.White
                )
                StatItem(
                    icon = Icons.Default.Speed,
                    value = "${statistics.avgDurationMinutes}",
                    label = "Phút TB",
                    textColor = Color.White
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    textColor: Color = TextPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(textColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun FrequentExerciseItem(exercise: ExerciseFrequency) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PrimaryOrange.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${exercise.totalCount} lần",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryOrange
            )
            Text(
                text = "${exercise.totalSets} sets",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun CalendarTab(
    calendarDays: List<CalendarDay>,
    selectedYearMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (String) -> Unit = {},
    selectedDateSessions: List<WorkoutSession> = emptyList(),
    selectedDatePlannedExercises: List<WorkoutPlanExercise> = emptyList(),
    selectedDateCompletedExercises: List<WorkoutExerciseDetail> = emptyList(),
    selectedDate: String? = null,
    isLoadingDaySessions: Boolean = false,
    onDismissSessionsDialog: () -> Unit = {},
    onViewSessionDetail: (Long) -> Unit = {}
) {
    Column {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(selectedYearMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Tháng trước")
            }
            Text(
                text = "${selectedYearMonth.month.toVietnamese()} ${selectedYearMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onMonthChange(selectedYearMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Tháng sau")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        CalendarGrid(
            yearMonth = selectedYearMonth,
            calendarDays = calendarDays,
            onDayClick = onDayClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendItem(color = MaterialTheme.colorScheme.primary, label = "Có tập")
            Spacer(modifier = Modifier.width(24.dp))
            LegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "Không tập")
        }
    }
    
    // Sessions dialog when clicking on a day
    if (selectedDate != null) {
        AlertDialog(
            onDismissRequest = onDismissSessionsDialog,
            title = { 
                Text(
                    "Chi tiết ngày ${selectedDate.split("-").reversed().joinToString("/")}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (isLoadingDaySessions) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        // 1. Planned
                        Text(
                            text = "Bài tập theo kế hoạch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (selectedDatePlannedExercises.isEmpty()) {
                            Text("Không có bài tập nào", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        } else {
                            selectedDatePlannedExercises.forEach { exercise ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${exercise.exerciseName} (${exercise.targetSets} sets)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        
                        // 2. Completed exercises
                        Text(
                            text = "Bài tập đã hoàn thành",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (selectedDateCompletedExercises.isEmpty()) {
                            Text(
                                text = "Chưa hoàn thành bài tập nào",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            // Show completed exercises with set details
                            selectedDateCompletedExercises.forEach { exercise ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            SuccessGreen.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    // Exercise header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = exercise.exerciseName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = "${exercise.sets.size} hiệp",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    
                                    // Set details
                                    if (exercise.sets.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(
                                            color = SuccessGreen.copy(alpha = 0.2f),
                                            thickness = 1.dp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        exercise.sets.forEachIndexed { index, set ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .background(
                                                                SuccessGreen.copy(alpha = 0.2f),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${index + 1}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SuccessGreen
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Hiệp ${index + 1}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextPrimary
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Reps
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "${set.reps}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SuccessGreen
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "reps",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                    // Weight
                                                    set.weightKg?.let { weight ->
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "$weight",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SuccessGreen
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "kg",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = TextSecondary
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
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissSessionsDialog) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    calendarDays: List<CalendarDay>,
    onDayClick: (String) -> Unit = {}
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    
    // Adjust for Monday start (1 = Monday, 7 = Sunday)
    val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7
    
    val calendarDayMap = calendarDays.associateBy { it.date }
    val today = LocalDate.now()
    
    val totalCells = startDayOfWeek + lastDayOfMonth.dayOfMonth
    val rows = (totalCells + 6) / 7
    
    Column {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - startDayOfWeek + 1
                    
                    if (dayOfMonth < 1 || dayOfMonth > lastDayOfMonth.dayOfMonth) {
                        // Empty cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val date = yearMonth.atDay(dayOfMonth)
                        val calendarDay = calendarDayMap[date.toString()]
                        val hasWorkout = calendarDay?.hasWorkout == true
                        val isToday = date == today
                        val isPast = date.isBefore(today)
                        
                        CalendarDayCell(
                            day = dayOfMonth,
                            hasWorkout = hasWorkout,
                            isToday = isToday,
                            isPast = isPast,
                            workoutCount = calendarDay?.workoutCount ?: 0,
                            onClick = if (hasWorkout) { { onDayClick(date.toString()) } } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    hasWorkout: Boolean,
    isToday: Boolean,
    isPast: Boolean = false,
    workoutCount: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    hasWorkout -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    // Today with workout -> white text
                    hasWorkout && isToday -> Color.White
                    // Past days with workout -> white text
                    hasWorkout && isPast -> Color.White
                    // Future with workout -> white text
                    hasWorkout -> Color.White
                    // Today without workout
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (hasWorkout && workoutCount > 1) {
                Text(
                    text = "×$workoutCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseChartTab(
    exerciseTemplates: List<ExerciseTemplate>,
    exerciseStats: ExerciseStatistics?,
    isLoading: Boolean,
    onLoadExerciseStats: (String, String?, String?) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedExercise by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var exerciseExpanded by remember { mutableStateOf(false) }
    
    // Date range state
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val groupedTemplates = remember(exerciseTemplates) {
        exerciseTemplates.groupBy { it.categoryName }
    }
    
    val categories = groupedTemplates.keys.toList()
    val exercisesInCategory = selectedCategory?.let { groupedTemplates[it] } ?: emptyList()
    
    // Date pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate?.atStartOfDay()?.toInstant(java.time.ZoneOffset.UTC)?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        selectedExercise?.let { exercise ->
                            onLoadExerciseStats(exercise, startDate?.toString(), endDate?.toString())
                        }
                    }
                    showStartDatePicker = false 
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate?.atStartOfDay()?.toInstant(java.time.ZoneOffset.UTC)?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerState.selectedDateMillis?.let { millis ->
                        endDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        selectedExercise?.let { exercise ->
                            onLoadExerciseStats(exercise, startDate?.toString(), endDate?.toString())
                        }
                    }
                    showEndDatePicker = false 
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tiến trình tạ theo bài tập",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Chọn nhóm cơ và bài tập để xem sự thay đổi mức tạ theo thời gian",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        // Category selector
        item {
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory ?: "Chọn nhóm cơ",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nhóm cơ") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                selectedExercise = null
                                categoryExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = PrimaryOrange
                                )
                            }
                        )
                    }
                }
            }
        }
        
        // Exercise selector
        if (selectedCategory != null) {
            item {
                ExposedDropdownMenuBox(
                    expanded = exerciseExpanded,
                    onExpandedChange = { exerciseExpanded = !exerciseExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedExercise ?: "Chọn bài tập",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bài tập") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exerciseExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = exerciseExpanded,
                        onDismissRequest = { exerciseExpanded = false }
                    ) {
                        exercisesInCategory.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    selectedExercise = template.name
                                    exerciseExpanded = false
                                    onLoadExerciseStats(template.name, startDate?.toString(), endDate?.toString())
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Date range picker - only show when exercise is selected
        if (selectedExercise != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            PrimaryOrange.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Khoảng thời gian",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryOrange
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start date
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = startDate?.let { "${it.dayOfMonth}/${it.monthValue}/${it.year}" } ?: "Từ ngày",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // End date
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = endDate?.let { "${it.dayOfMonth}/${it.monthValue}/${it.year}" } ?: "Đến ngày",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Clear date range
                    if (startDate != null || endDate != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                startDate = null
                                endDate = null
                                selectedExercise?.let { onLoadExerciseStats(it, null, null) }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xóa bộ lọc", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        
        // Chart
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (selectedExercise == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.BarChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Chọn bài tập để xem biểu đồ",
                                    color = TextSecondary
                                )
                            }
                        }
                    } else if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (exerciseStats != null && exerciseStats.weightHistory.isNotEmpty()) {
                        Text(
                            text = selectedExercise ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Max weight info
                        exerciseStats.maxWeightKg?.let { maxWeight ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tạ cao nhất:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${maxWeight}kg",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryOrange
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Weight progression chart
                        WeightProgressionChart(
                            weightHistory = exerciseStats.weightHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Dữ liệu ${exerciseStats.weightHistory.size} ngày gần nhất",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có dữ liệu cho bài tập này",
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightProgressionChart(
    weightHistory: List<WeightHistoryEntry>,
    modifier: Modifier = Modifier
) {
    val primaryColor = PrimaryOrange
    
    if (weightHistory.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("Không có dữ liệu", color = TextSecondary)
        }
        return
    }
    
    // State for selected data point tooltip
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    val maxWeight = weightHistory.maxOfOrNull { it.maxWeightKg } ?: 1.0
    val minWeight = weightHistory.minOfOrNull { it.maxWeightKg } ?: 0.0
    val weightRange = (maxWeight - minWeight).takeIf { it > 0 } ?: 1.0
    
    // Calculate nice Y axis ticks
    val yTicks = remember(maxWeight, minWeight) {
        val step = ((maxWeight - minWeight) / 4).coerceAtLeast(1.0)
        val niceStep = when {
            step <= 1 -> 1.0
            step <= 2.5 -> 2.5
            step <= 5 -> 5.0
            step <= 10 -> 10.0
            else -> (step / 10).toInt() * 10.0
        }
        val start = (minWeight / niceStep).toInt() * niceStep
        val end = ((maxWeight / niceStep).toInt() + 1) * niceStep
        generateSequence(start) { it + niceStep }.takeWhile { it <= end }.toList()
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedIndex != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedIndex?.let { idx ->
                val entry = weightHistory.getOrNull(idx)
                entry?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = try {
                                        val parts = entry.date.split("-")
                                        "${parts.getOrNull(2) ?: ""}/${parts.getOrNull(1) ?: ""}/${parts.getOrNull(0) ?: ""}"
                                    } catch (e: Exception) { entry.date },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Ngày tập",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(DividerLight)
                            )
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${entry.maxWeightKg} kg",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Text(
                                    text = "Tạ cao nhất",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            
                            entry.totalSets.takeIf { it > 0 }?.let { sets ->
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(DividerLight)
                                )
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$sets",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "Hiệp",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth()) {
            // Y-axis label (rotated)
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.graphicsLayer { rotationZ = -90f }
                )
            }
            
            // Y-axis values
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .height(140.dp)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                yTicks.reversed().forEach { value ->
                    Text(
                        text = "${value.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Chart area with touch detection
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .pointerInput(weightHistory) {
                            detectTapGestures { offset ->
                                val spacing = size.width.toFloat() / (weightHistory.size + 1)
                                val clickedIndex = ((offset.x / spacing) - 0.5f).toInt().coerceIn(0, weightHistory.size - 1)
                                selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val paddingTop = 8f
                        val paddingBottom = 8f
                        
                        val chartHeight = height - paddingTop - paddingBottom
                        val spacing = width / (weightHistory.size + 1)
                        
                        // Draw horizontal grid lines
                        yTicks.forEachIndexed { index, _ ->
                            val y = paddingTop + (index.toFloat() / (yTicks.size - 1).coerceAtLeast(1)) * chartHeight
                            drawLine(
                                color = primaryColor.copy(alpha = 0.1f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }
                        
                        // Draw X axis
                        drawLine(
                            color = primaryColor.copy(alpha = 0.3f),
                            start = Offset(0f, height - paddingBottom),
                            end = Offset(width, height - paddingBottom),
                            strokeWidth = 2f
                        )
                        
                        // Draw line chart
                        val path = Path()
                        val pointPositions = mutableListOf<Offset>()
                        
                        weightHistory.forEachIndexed { index, entry ->
                            val x = (index + 1) * spacing
                            val normalizedWeight = (entry.maxWeightKg - minWeight) / weightRange
                            val y = height - paddingBottom - (normalizedWeight * chartHeight * 0.95f).toFloat()
                            
                            pointPositions.add(Offset(x, y))
                            
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        
                        // Draw gradient area under line
                        if (pointPositions.isNotEmpty()) {
                            val areaPath = Path().apply {
                                addPath(path)
                                lineTo(pointPositions.last().x, height - paddingBottom)
                                lineTo(pointPositions.first().x, height - paddingBottom)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.3f),
                                        primaryColor.copy(alpha = 0.05f)
                                    )
                                )
                            )
                        }
                        
                        // Draw line
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        
                        // Draw points
                        pointPositions.forEachIndexed { index, point ->
                            val isSelected = selectedIndex == index
                            val pointRadius = if (isSelected) 10f else 6f
                            val glowRadius = if (isSelected) 18f else 12f
                            
                            // Glow
                            drawCircle(
                                color = primaryColor.copy(alpha = if (isSelected) 0.4f else 0.3f),
                                radius = glowRadius,
                                center = point
                            )
                            // Point
                            drawCircle(
                                color = if (isSelected) primaryColor else primaryColor,
                                radius = pointRadius,
                                center = point
                            )
                            // Inner dot
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 5f else 3f,
                                center = point
                            )
                        }
                    }
                }
                
                // X-axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weightHistory.takeLast(5).forEachIndexed { index, entry ->
                        val dateText = try {
                            val parts = entry.date.split("-")
                            if (parts.size >= 2) "${parts.getOrNull(2) ?: ""}/${parts.getOrNull(1) ?: ""}"
                            else entry.date.takeLast(5)
                        } catch (e: Exception) {
                            entry.date.takeLast(5)
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
                
                // X-axis label
                Text(
                    text = "Ngày",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ChartTab(
    weeklyProgress: List<WeeklyProgress>,
    workoutHistory: List<DailyWorkoutSummary>,
    onLoadHistory: (Int) -> Unit
) {
    var selectedDays by remember { mutableStateOf(30) }
    
    LaunchedEffect(selectedDays) {
        onLoadHistory(selectedDays)
    }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Weekly progress chart
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tiến độ theo tuần",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (weeklyProgress.isNotEmpty()) {
                        WeeklyProgressChart(
                            data = weeklyProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có dữ liệu",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Duration filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDays == 7,
                    onClick = { selectedDays = 7 },
                    label = { Text("7 ngày") }
                )
                FilterChip(
                    selected = selectedDays == 30,
                    onClick = { selectedDays = 30 },
                    label = { Text("30 ngày") }
                )
                FilterChip(
                    selected = selectedDays == 90,
                    onClick = { selectedDays = 90 },
                    label = { Text("90 ngày") }
                )
            }
        }
        
        // Daily workout bar chart
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lịch sử tập luyện",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (workoutHistory.isNotEmpty()) {
                        DailyWorkoutChart(
                            data = workoutHistory.takeLast(14),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có dữ liệu",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyProgressChart(
    data: List<WeeklyProgress>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40f
        
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2
        
        val maxWorkouts = data.maxOfOrNull { it.totalWorkouts } ?: 1
        val maxMinutes = data.maxOfOrNull { it.totalMinutes } ?: 1
        
        val barWidth = chartWidth / data.size * 0.6f
        val spacing = chartWidth / data.size
        
        // Draw bars
        data.forEachIndexed { index, week ->
            val x = padding + index * spacing + spacing / 2 - barWidth / 2
            
            // Workout count bar
            val workoutHeight = (week.totalWorkouts.toFloat() / maxWorkouts) * chartHeight * 0.8f
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, height - padding - workoutHeight),
                size = Size(barWidth * 0.4f, workoutHeight)
            )
            
            // Minutes bar
            val minutesHeight = (week.totalMinutes.toFloat() / maxMinutes) * chartHeight * 0.8f
            drawRect(
                color = secondaryColor.copy(alpha = 0.6f),
                topLeft = Offset(x + barWidth * 0.5f, height - padding - minutesHeight),
                size = Size(barWidth * 0.4f, minutesHeight)
            )
        }
        
        // Draw axis
        drawLine(
            color = primaryColor.copy(alpha = 0.3f),
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
    }
    
    // Legend
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendItem(color = primaryColor, label = "Buổi tập")
        Spacer(modifier = Modifier.width(24.dp))
        LegendItem(color = secondaryColor.copy(alpha = 0.6f), label = "Phút tập")
    }
}

@Composable
fun DailyWorkoutChart(
    data: List<DailyWorkoutSummary>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 20f
        
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2
        
        val maxDuration = data.maxOfOrNull { it.totalDuration } ?: 1
        
        val barWidth = chartWidth / data.size * 0.7f
        val spacing = chartWidth / data.size
        
        data.forEachIndexed { index, day ->
            val x = padding + index * spacing + spacing / 2 - barWidth / 2
            val barHeight = (day.totalDuration.toFloat() / maxDuration) * chartHeight * 0.85f
            
            val color = if (day.workoutCount > 0) primaryColor else primaryColor.copy(alpha = 0.2f)
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, height - padding - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}

// Extension function
fun java.time.Month.toVietnamese(): String = when (this) {
    java.time.Month.JANUARY -> "Tháng 1"
    java.time.Month.FEBRUARY -> "Tháng 2"
    java.time.Month.MARCH -> "Tháng 3"
    java.time.Month.APRIL -> "Tháng 4"
    java.time.Month.MAY -> "Tháng 5"
    java.time.Month.JUNE -> "Tháng 6"
    java.time.Month.JULY -> "Tháng 7"
    java.time.Month.AUGUST -> "Tháng 8"
    java.time.Month.SEPTEMBER -> "Tháng 9"
    java.time.Month.OCTOBER -> "Tháng 10"
    java.time.Month.NOVEMBER -> "Tháng 11"
    java.time.Month.DECEMBER -> "Tháng 12"
}

fun java.time.DayOfWeek.toVietnamese(): String = when (this) {
    java.time.DayOfWeek.MONDAY -> "Thứ 2"
    java.time.DayOfWeek.TUESDAY -> "Thứ 3"
    java.time.DayOfWeek.WEDNESDAY -> "Thứ 4"
    java.time.DayOfWeek.THURSDAY -> "Thứ 5"
    java.time.DayOfWeek.FRIDAY -> "Thứ 6"
    java.time.DayOfWeek.SATURDAY -> "Thứ 7"
    java.time.DayOfWeek.SUNDAY -> "Chủ nhật"
}

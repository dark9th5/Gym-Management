package com.lc9th5.gym.ui.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.lc9th5.gym.data.model.*
import com.lc9th5.gym.ui.theme.*
import com.lc9th5.gym.viewmodel.WorkoutUiState
import com.lc9th5.gym.viewmodel.WorkoutViewModel
import com.lc9th5.gym.viewmodel.SelectedExercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    
    // State for edit pending exercise dialog
    var exerciseToEdit by remember { mutableStateOf<WorkoutPlanExercise?>(null) }
    // State for edit session exercise (in progress/undo/added)
    var sessionExerciseToEdit by remember { mutableStateOf<WorkoutExerciseDetail?>(null) }
    
    // State for draggable FAB
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }
    
    // Check if workout is already saved (has completed session today)
    val hasCompletedTodayWorkout = uiState.todaySessions.any { it.endedAt != null }
    
    LaunchedEffect(Unit) {
        viewModel.loadStreak()
        viewModel.loadSessions()
        viewModel.loadTodayPlan() // Ensure plan is loaded
        viewModel.loadTodaySessions() // Load today's sessions
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            // Only show FAB if workout is NOT completed yet
            if (!hasCompletedTodayWorkout) {
                FloatingActionButton(
                    onClick = { showAddExerciseDialog = true },
                    containerColor = PrimaryOrange,
                    contentColor = Color.White,
                    modifier = Modifier
                        .offset { IntOffset(fabOffsetX.toInt(), fabOffsetY.toInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                fabOffsetX += dragAmount.x
                                fabOffsetY += dragAmount.y
                            }
                        }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm bài tập")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 8.dp)
        ) {
            // Premium Today Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (hasCompletedTodayWorkout)
                            Brush.horizontalGradient(listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.8f)))
                        else
                            Brush.horizontalGradient(listOf(PrimaryOrange, SecondaryOrange))
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (hasCompletedTodayWorkout) "HOÀN THÀNH!" else "TODAY",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (hasCompletedTodayWorkout) {
                            Text(
                                text = "Tuyệt vời! Bạn đã hoàn thành buổi tập",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        } else {
                            val sessionExercises = uiState.currentSessionDetail?.exercises ?: emptyList()
                            val completedCount = sessionExercises.count { it.sets.all { set -> set.isCompleted } && it.sets.isNotEmpty() }
                            val totalCount = uiState.pendingExercises.size + sessionExercises.size
                            if (totalCount > 0) {
                                Text(
                                    text = "$completedCount / $totalCount bài tập",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    
                    if (hasCompletedTodayWorkout) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        // Save button integrated into header
                        Button(
                            onClick = { showSaveConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PrimaryOrange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lưu", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

                // ===== UI OPTIMIZATION: LazyColumn with keys for efficient recomposition =====
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val sessionExercises = uiState.currentSessionDetail?.exercises ?: emptyList()
                    val inProgressExercises = sessionExercises.filter { it.sets.any { !it.isCompleted } }
                    val completedExercises = sessionExercises.filter { it.sets.all { set -> set.isCompleted } && it.sets.isNotEmpty() }
                    
                    // Only show "Chưa tập" section if workout is NOT completed
                    if (!hasCompletedTodayWorkout) {
                        item {
                            Text(
                                text = "Chưa tập",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        if (uiState.pendingExercises.isNotEmpty() || inProgressExercises.isNotEmpty()) {
                            if (uiState.pendingExercises.isNotEmpty()) {
                                items(
                                    items = uiState.pendingExercises,
                                    key = { exercise -> "pending_${exercise.id}" }
                                ) { exercise ->
                                    PendingExerciseItem(
                                        exercise = exercise,
                                        onComplete = { viewModel.completePendingExercise(exercise) },
                                        onEdit = { exerciseToEdit = exercise },
                                        onDelete = { viewModel.deletePendingExercise(exercise.id) }
                                    )
                                }
                            }
                            if (inProgressExercises.isNotEmpty()) {
                                items(
                                    items = inProgressExercises,
                                    key = { exercise -> "inprogress_${exercise.id}" }
                                ) { exercise ->
                                    InProgressExerciseItem(
                                        exercise = exercise,
                                        onComplete = { viewModel.markExerciseSetsCompleted(exercise) },
                                        onEdit = { sessionExerciseToEdit = exercise },
                                        onDelete = { viewModel.removeCompletedExercise(exercise.id) }
                                    )
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = "Không có bài tập chưa hoàn thành",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                                )
                            }
                        }
                    }

                    // Completed Section - Always show
                    item {
                        Text(
                            text = "Đã tập",
                            style = MaterialTheme.typography.titleMedium,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    if (completedExercises.isNotEmpty()) {
                        items(
                            items = completedExercises,
                            key = { exercise -> "completed_${exercise.id}" }
                        ) { exercise ->
                            CompletedExerciseItem(
                                exercise = exercise,
                                onUndo = if (!hasCompletedTodayWorkout) {{ viewModel.undoCompletedExercise(exercise) }} else null,
                                onDelete = if (!hasCompletedTodayWorkout) {{ viewModel.removeCompletedExercise(exercise.id) }} else null
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = if (hasCompletedTodayWorkout) "Hôm nay bạn đã hoàn thành buổi tập!" else "Chưa hoàn thành bài tập nào",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasCompletedTodayWorkout) SuccessGreen else TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                    
                    // Bottom padding for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    
    // Dialogs
    if (showAddExerciseDialog) {
        LaunchedEffect(Unit) {
            viewModel.loadExerciseTemplates()
        }
        
        AddExerciseDialog(
            exerciseTemplates = uiState.exerciseTemplates,
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { name, lessonId, setRows, notes, scheduledTime ->
                viewModel.addExerciseToSessionWithSets(name, lessonId, setRows, notes, scheduledTime)
                showAddExerciseDialog = false
            }
        )
    }
    
    // Save Confirmation Dialog
    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Xác nhận lưu buổi tập",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sau khi lưu, bạn sẽ không thể thay đổi bài tập hôm nay được nữa. Bạn có chắc chắn muốn lưu?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveWorkoutSession()
                        showSaveConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    )
                ) {
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
    
    // Edit Pending Exercise Dialog
    exerciseToEdit?.let { exercise ->
        EditPendingExerciseDialog(
            exercise = exercise,
            onDismiss = { exerciseToEdit = null },
            onConfirm = { updatedExercise ->
                viewModel.updatePendingExercise(updatedExercise)
                exerciseToEdit = null
            }
        )
    }
    
    // Edit Session Exercise Dialog
    sessionExerciseToEdit?.let { exercise ->
        EditSessionExerciseDialog(
            exercise = exercise,
            onDismiss = { sessionExerciseToEdit = null },
            onConfirm = { updatedSetRows ->
                viewModel.updateSessionExercise(exercise.id, updatedSetRows)
                sessionExerciseToEdit = null
            }
        )
    }
    
    // Error Snackbar
    uiState.error?.let {
        // ...
    }
}

@Composable
fun EditPendingExerciseDialog(
    exercise: WorkoutPlanExercise,
    onDismiss: () -> Unit,
    onConfirm: (WorkoutPlanExercise) -> Unit
) {
    // Parse existing sets from notes
    val parsedSets = remember(exercise.notes) { parseSetsFromNotes(exercise.notes) }
    
    // Create mutable list of sets
    var sets by remember {
        mutableStateOf(
            if (parsedSets.isNotEmpty()) {
                parsedSets.map { (reps, weight) ->
                    SetRowData(reps.toString(), weight?.toString() ?: "")
                }
            } else {
                // Create default sets based on targetSets
                (1..exercise.targetSets.coerceAtLeast(1)).map {
                    SetRowData(exercise.targetReps, exercise.targetWeightKg?.toString() ?: "")
                }
            }
        )
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sửa bài tập",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Exercise name (read-only)
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryOrange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sets table header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hiệp",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(50.dp)
                    )
                    Text(
                        text = "Rep",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tạ (kg)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Sets rows
                sets.forEachIndexed { index, set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Set number
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimaryOrange.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange
                            )
                        }
                        
                        // Reps input with +/- buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val currentReps = set.reps.toIntOrNull() ?: 0
                                    if (currentReps > 1) {
                                        sets = sets.toMutableList().apply {
                                            this[index] = set.copy(reps = (currentReps - 1).toString())
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
                            }
                            OutlinedTextField(
                                value = set.reps,
                                onValueChange = { newValue ->
                                    sets = sets.toMutableList().apply {
                                        this[index] = set.copy(reps = newValue.filter { it.isDigit() })
                                    }
                                },
                                modifier = Modifier.width(60.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val currentReps = set.reps.toIntOrNull() ?: 0
                                    sets = sets.toMutableList().apply {
                                        this[index] = set.copy(reps = (currentReps + 1).toString())
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
                                    val currentWeight = set.weight.toDoubleOrNull() ?: 0.0
                                    if (currentWeight > 0) {
                                        sets = sets.toMutableList().apply {
                                            this[index] = set.copy(weight = (currentWeight - 2.5).coerceAtLeast(0.0).toString())
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
                            }
                            OutlinedTextField(
                                value = set.weight,
                                onValueChange = { newValue ->
                                    sets = sets.toMutableList().apply {
                                        this[index] = set.copy(weight = newValue.filter { it.isDigit() || it == '.' })
                                    }
                                },
                                modifier = Modifier.width(70.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val currentWeight = set.weight.toDoubleOrNull() ?: 0.0
                                    sets = sets.toMutableList().apply {
                                        this[index] = set.copy(weight = (currentWeight + 2.5).toString())
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Add/Remove set buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            if (sets.size > 1) {
                                sets = sets.dropLast(1)
                            }
                        },
                        enabled = sets.size > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bớt hiệp")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val lastSet = sets.lastOrNull()
                            sets = sets + SetRowData(
                                lastSet?.reps ?: "10",
                                lastSet?.weight ?: ""
                            )
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm hiệp")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy")
                    }
                    
                    Button(
                        onClick = {
                            // Build notes string from sets
                            val notesStr = sets.joinToString("|") { set ->
                                "${set.reps}x${set.weight.takeIf { it.isNotBlank() } ?: "0"}"
                            }
                            val updatedExercise = exercise.copy(
                                targetSets = sets.size,
                                notes = notesStr
                            )
                            onConfirm(updatedExercise)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Lưu thay đổi")
                    }
                }
            }
        }
    }
}

@Composable
fun EditSessionExerciseDialog(
    exercise: WorkoutExerciseDetail,
    onDismiss: () -> Unit,
    onConfirm: (List<SetRowData>) -> Unit
) {
    // Initialize sets from existing session sets
    var sets by remember {
        mutableStateOf(
            if (exercise.sets.isNotEmpty()) {
                exercise.sets.map { set ->
                    SetRowData(set.reps.toString(), set.weightKg?.toString() ?: "")
                }
            } else {
                listOf(SetRowData("10", ""))
            }
        )
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sửa bài tập",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Exercise name (read-only)
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryOrange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sets table header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hiệp",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(50.dp)
                    )
                    Text(
                        text = "Rep",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tạ (kg)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Sets rows
                sets.forEachIndexed { index, set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Set number
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimaryOrange.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange
                            )
                        }
                        
                        // Reps input with +/- buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val currentReps = set.reps.toIntOrNull() ?: 0
                                    if (currentReps > 1) {
                                        sets = sets.toMutableList().apply {
                                            this[index] = set.copy(reps = (currentReps - 1).toString())
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
                            }
                            OutlinedTextField(
                                value = set.reps,
                                onValueChange = { newValue ->
                                    sets = sets.toMutableList().apply {
                                        this[index] = set.copy(reps = newValue.filter { it.isDigit() })
                                    }
                                },
                                modifier = Modifier.width(60.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val currentReps = set.reps.toIntOrNull() ?: 0
                                    sets = sets.toMutableList().apply {
                                        this[index] = set.copy(reps = (currentReps + 1).toString())
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
                                    val currentWeight = set.weight.toDoubleOrNull() ?: 0.0
                                    if (currentWeight > 0) {
                                        sets = sets.toMutableList().apply {
                                            this[index] = set.copy(weight = (currentWeight - 2.5).coerceAtLeast(0.0).toString())
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
                            }
                            OutlinedTextField(
                                value = set.weight,
                                onValueChange = { newValue ->
                                    sets = sets.toMutableList().apply {
                                        this[index] = set.copy(weight = newValue.filter { it.isDigit() || it == '.' })
                                    }
                                },
                                modifier = Modifier.width(70.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val currentWeight = set.weight.toDoubleOrNull() ?: 0.0
                                    sets = sets.toMutableList().apply {
                                        this[index] = set.copy(weight = (currentWeight + 2.5).toString())
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Add/Remove set buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            if (sets.size > 1) {
                                sets = sets.dropLast(1)
                            }
                        },
                        enabled = sets.size > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bớt hiệp")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val lastSet = sets.lastOrNull()
                            sets = sets + SetRowData(
                                lastSet?.reps ?: "10",
                                lastSet?.weight ?: ""
                            )
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm hiệp")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy")
                    }
                    
                    Button(
                        onClick = {
                            onConfirm(sets)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Lưu thay đổi")
                    }
                }
            }
        }
    }
}

@Composable
fun PendingExerciseItem(
    exercise: WorkoutPlanExercise,
    onComplete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    scheduledTime: String? = null // Optional scheduled time from reminder
) {
    var isExpanded by remember { mutableStateOf(false) }
    val parsedSets = remember(exercise.notes) { parseSetsFromNotes(exercise.notes) }
    val hasSets = parsedSets.isNotEmpty() || exercise.targetSets > 0
    
    // Unified styling with other exercise items
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                color = PrimaryOrange.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { if (hasSets) isExpanded = !isExpanded }
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pending indicator - similar to InProgressExerciseItem
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = PrimaryOrange.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sets summary
                        if (parsedSets.isNotEmpty()) {
                            Text(
                                text = "${parsedSets.size} hiệp",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        } else if (exercise.targetSets > 0) {
                            Text(
                                text = "${exercise.targetSets} × ${exercise.targetReps}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        
                        // Scheduled time badge
                        scheduledTime?.let { time ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = PrimaryOrange.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryOrange
                                )
                            }
                        }
                    }
                }
            }
            
            // Action buttons row - matches InProgressExerciseItem style
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Complete button
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle, 
                        contentDescription = "Hoàn thành", 
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Edit button (optional)
                onEdit?.let { editAction ->
                    IconButton(
                        onClick = editAction,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Sửa", 
                            tint = PrimaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Delete button (optional)
                onDelete?.let { deleteAction ->
                    IconButton(
                        onClick = deleteAction,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Xóa", 
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Expand indicator
                if (hasSets) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }
        }
        
        // Expanded sets details
        if (isExpanded && hasSets) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerLight)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (parsedSets.isNotEmpty()) {
                parsedSets.forEachIndexed { idx, set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                PrimaryOrange.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hiệp ${idx + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${set.first}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryOrange
                                )
                                Text(
                                    text = "reps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            set.second?.let { weight ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$weight",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryOrange
                                    )
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
            } else if (exercise.targetSets > 0) {
                Text(
                    text = "Mục tiêu: ${exercise.targetSets} hiệp × ${exercise.targetReps} reps" +
                            (exercise.targetWeightKg?.let { " @ ${it}kg" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun InProgressExerciseItem(
    exercise: WorkoutExerciseDetail,
    onComplete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasSets = exercise.sets.isNotEmpty()
    
    // ===== OVERDRAW NOTE: Low alpha (0.08f) background minimizes overdraw impact =====
    // Nested backgrounds with very low alpha are visually necessary but have minimal GPU cost
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                color = SecondaryOrange.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { if (hasSets) isExpanded = !isExpanded }
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // In-progress indicator
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = SecondaryOrange.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SecondaryOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${exercise.sets.size} hiệp • ${exercise.sets.sumOf { it.reps }} reps",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Complete button
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle, 
                        contentDescription = "Hoàn thành", 
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Edit button (optional)
                onEdit?.let { editAction ->
                    IconButton(
                        onClick = editAction,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Sửa", 
                            tint = PrimaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Xóa", 
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Expand indicator
                if (hasSets) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }
        }
        
        // Expanded sets details
        if (isExpanded && hasSets) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SecondaryOrange.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))
            
            exercise.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            SecondaryOrange.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(SecondaryOrange.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Hiệp ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${set.reps}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryOrange
                            )
                            Text(
                                text = "reps",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        set.weightKg?.let { weight ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$weight",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryOrange
                                )
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

@Composable
fun CompletedExerciseItem(
    exercise: WorkoutExerciseDetail,
    onUndo: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // ===== OVERDRAW NOTE: Low alpha (0.08f) background minimizes overdraw impact =====
    // Nested backgrounds with very low alpha are visually necessary but have minimal GPU cost
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                color = SuccessGreen.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Completed checkmark
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(SuccessGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${exercise.sets.size} hiệp • ${exercise.sets.sumOf { it.reps }} reps",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo button (move back to pending)
                onUndo?.let { undoAction ->
                    IconButton(
                        onClick = undoAction,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Chuyển về chưa tập",
                            tint = SecondaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Delete button (only show if onDelete provided)
                onDelete?.let { deleteAction ->
                    IconButton(
                        onClick = deleteAction,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Expand/Collapse indicator
                if (exercise.sets.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }
        }
        
        // Expanded content - Set details
        if (isExpanded && exercise.sets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SuccessGreen.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))
            
            exercise.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            SuccessGreen.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(SuccessGreen.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Hiệp ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${set.reps}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text(
                                text = "reps",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        set.weightKg?.let { weight ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$weight",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
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


@Composable
fun StreakCard(streak: StreakSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PrimaryOrange, SecondaryOrange, AccentCoral)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreakItem(
                icon = Icons.Default.LocalFireDepartment,
                value = streak.currentStreak.toString(),
                label = "Chuỗi hiện tại",
                iconColor = Color.White
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            StreakItem(
                icon = Icons.Default.EmojiEvents,
                value = streak.longestStreak.toString(),
                label = "Kỷ lục",
                iconColor = Color.White
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            StreakItem(
                icon = Icons.Default.FitnessCenter,
                value = streak.totalWorkouts.toString(),
                label = "Tổng buổi tập",
                iconColor = Color.White
            )
        }
    }
}

@Composable
fun StreakItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconColor: Color = PrimaryOrange
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}



@Composable
fun SessionCard(
    session: WorkoutSession,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                Column {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        session.durationMinutes?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${it} phút",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        session.caloriesBurned?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = AccentCoral
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${it} cal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            
            onDelete?.let {
                IconButton(onClick = it) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = ErrorRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySessionsMessage() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PrimaryOrange.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = PrimaryOrange
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Chưa có buổi tập nào",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Bắt đầu tập luyện ngay hôm nay!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartSessionDialog(
    allowBackfill: Boolean = false,
    exerciseTemplates: List<ExerciseTemplate> = emptyList(),
    onLoadTemplates: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (exercises: List<SelectedExercise>, startedAt: String?) -> Unit
) {
    var selectedExercises by remember { mutableStateOf<List<SelectedExercise>>(emptyList()) }
    var isBackfill by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        onLoadTemplates()
    }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    
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
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                        text = if (isBackfill) "Ghi bù lịch sử" else "Bắt đầu buổi tập",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Tìm bài tập...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Xóa")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Exercise list by category
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedTemplates.forEach { (category, templates) ->
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        expandedCategory = if (expandedCategory == category) null else category 
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = PrimaryOrange.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = PrimaryOrange,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryOrange
                                        )
                                    }
                                    Icon(
                                        if (expandedCategory == category) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = PrimaryOrange
                                    )
                                }
                            }
                        }
                        
                        if (expandedCategory == category) {
                            items(templates) { template ->
                                val isSelected = selectedExercises.any { it.templateId == template.id }
                                val exercise = selectedExercises.find { it.templateId == template.id }
                                
                                ExerciseSelectionCard(
                                    template = template,
                                    isSelected = isSelected,
                                    sets = exercise?.sets ?: 3,
                                    reps = exercise?.reps ?: 8,
                                    weight = exercise?.weight ?: "",
                                    onToggle = {
                                        selectedExercises = if (isSelected) {
                                            selectedExercises.filter { it.templateId != template.id }
                                        } else {
                                            selectedExercises + SelectedExercise(
                                                templateId = template.id,
                                                name = template.name,
                                                sets = 3,
                                                reps = 8,
                                                weight = ""
                                            )
                                        }
                                    },
                                    onSetsChange = { newSets ->
                                        selectedExercises = selectedExercises.map {
                                            if (it.templateId == template.id) it.copy(sets = newSets) else it
                                        }
                                    },
                                    onRepsChange = { newReps ->
                                        selectedExercises = selectedExercises.map {
                                            if (it.templateId == template.id) it.copy(reps = newReps) else it
                                        }
                                    },
                                    onWeightChange = { newWeight ->
                                        selectedExercises = selectedExercises.map {
                                            if (it.templateId == template.id) it.copy(weight = newWeight) else it
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Selected count
                if (selectedExercises.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SuccessGreen.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đã chọn ${selectedExercises.size} bài tập",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = SuccessGreen
                            )
                        }
                    }
                }
                
                // Backfill option
                if (allowBackfill) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isBackfill,
                            onCheckedChange = { isBackfill = it }
                        )
                        Text("Ghi lại buổi tập đã bỏ lỡ")
                    }
                    
                    if (isBackfill) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedDate?.let {
                                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                        .format(java.util.Date(it))
                                } ?: "Chọn ngày"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
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
                            if (selectedExercises.isNotEmpty()) {
                                val startedAt = if (isBackfill && selectedDate != null) {
                                    val calendar = java.util.Calendar.getInstance()
                                    calendar.timeInMillis = selectedDate!!
                                    String.format(
                                        "%04d-%02d-%02dT12:00:00",
                                        calendar.get(java.util.Calendar.YEAR),
                                        calendar.get(java.util.Calendar.MONTH) + 1,
                                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    )
                                } else null
                                onConfirm(selectedExercises, startedAt)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedExercises.isNotEmpty() && (!isBackfill || selectedDate != null),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBackfill) "Ghi bù" else "Bắt đầu")
                    }
                }
            }
        }
    }
}



@Composable
fun ExerciseSelectionCard(
    template: ExerciseTemplate,
    isSelected: Boolean,
    sets: Int,
    reps: Int,
    weight: String,
    onToggle: () -> Unit,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SuccessGreen.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SuccessGreen
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sets control
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hiệp",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DividerLight, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (sets > 1) onSetsChange(sets - 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = sets.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onSetsChange(sets + 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    // Reps control
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rep/Hiệp",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DividerLight, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (reps > 1) onRepsChange(reps - 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = reps.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onRepsChange(reps + 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    // Weight input
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tạ (kg)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
    }
}



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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    exerciseTemplates: List<ExerciseTemplate> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, lessonId: Long?, sets: List<SetRowData>, notes: String?, scheduledTime: String?) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var exerciseName by remember { mutableStateOf("") }
    var exerciseNotes by remember { mutableStateOf("") }
    var useCustomName by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Individual set rows - default 3 sets
    var setRows by remember { mutableStateOf(listOf(
        SetRowData("8", ""),
        SetRowData("8", ""),
        SetRowData("8", "")
    )) }

    var showTimePicker by remember { mutableStateOf(false) }
    var scheduledHour by remember { mutableStateOf(7) }
    var scheduledMinute by remember { mutableStateOf(0) }
    var enableNotification by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = scheduledHour,
        initialMinute = scheduledMinute
    )
    
    val filteredTemplates = remember(searchQuery, exerciseTemplates) {
        if (searchQuery.isBlank()) exerciseTemplates
        else exerciseTemplates.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.categoryName.contains(searchQuery, ignoreCase = true)
        }
    }
    
    // Group templates by category
    val groupedTemplates = remember(filteredTemplates) {
        filteredTemplates.groupBy { it.categoryName }
    }
    
    // Validate: all sets must have reps and weight
    val isValid = exerciseName.isNotBlank() && setRows.all { 
        it.reps.isNotBlank() && it.weight.isNotBlank() 
    }
    
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
                        text = "Thêm bài tập",
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
                        // Search field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Tìm bài tập...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Xóa")
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Exercise list grouped by category
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            LazyColumn(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                groupedTemplates.forEach { (category, templates) ->
                                    item {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryOrange,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
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
                                                modifier = Modifier.padding(12.dp),
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
                        // Custom name input
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
                                    val lastSet = setRows.lastOrNull() ?: SetRowData()
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
                                modifier = Modifier.size(36.dp),
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Add/Remove set buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { 
                                if (setRows.size > 1) {
                                    setRows = setRows.dropLast(1) 
                                }
                            },
                            enabled = setRows.size > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bớt hiệp")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                val lastSet = setRows.lastOrNull()
                                setRows = setRows + SetRowData(
                                    lastSet?.reps ?: "10",
                                    lastSet?.weight ?: ""
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm hiệp")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Notification time section
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nhắc giờ tập",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gửi thông báo nhắc tập cho bài này",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = enableNotification,
                            onCheckedChange = { enableNotification = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryOrange
                            )
                        )
                    }

                    if (enableNotification) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Thời gian tập",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = String.format("%02d:%02d", scheduledHour, scheduledMinute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryOrange
                                    )
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Chỉnh",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Notes
                    OutlinedTextField(
                        value = exerciseNotes,
                        onValueChange = { exerciseNotes = it },
                        label = { Text("Ghi chú (tùy chọn)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
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
                                    exerciseNotes.ifBlank { null },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSetDialog(
    presets: ExercisePresets? = null,
    onDismiss: () -> Unit,
    onConfirm: (reps: Int, weight: Double?) -> Unit
) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Set") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Quick select reps
                if (presets != null) {
                    Text(
                        text = "Chọn nhanh số rep:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.commonReps.forEach { rep ->
                            FilterChip(
                                selected = reps == rep.toString(),
                                onClick = { reps = rep.toString() },
                                label = { Text("$rep") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it.filter { c -> c.isDigit() } },
                    label = { Text("Số lần (reps)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Trọng lượng (kg) - tùy chọn") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val repsInt = reps.toIntOrNull()
                    if (repsInt != null && repsInt > 0) {
                        onConfirm(repsInt, weight.toDoubleOrNull())
                    }
                },
                enabled = reps.toIntOrNull()?.let { it > 0 } == true
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailDialog(
    detail: WorkoutSessionDetail,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
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
                        text = detail.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Session stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    detail.durationMinutes?.let {
                        StatChip(icon = Icons.Default.Timer, value = "${it} phút")
                    }
                    detail.caloriesBurned?.let {
                        StatChip(icon = Icons.Default.LocalFireDepartment, value = "${it} cal")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Exercises
                Text(
                    text = "Bài tập (${detail.exercises.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(detail.exercises) { exercise ->
                        ExerciseDetailCard(exercise = exercise)
                    }
                }
                
                // Notes
                detail.notes?.let { notes ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ghi chú: $notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ExerciseDetailCard(exercise: WorkoutExerciseDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            if (exercise.sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                exercise.sets.forEachIndexed { index, set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Set ${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = buildString {
                                append("${set.reps} reps")
                                set.weightKg?.let { append(" × ${it}kg") }
                                set.durationSeconds?.let { append(" • ${it}s") }
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (set.isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Hoàn thành",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

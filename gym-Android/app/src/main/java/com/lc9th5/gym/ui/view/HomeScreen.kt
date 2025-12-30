package com.lc9th5.gym.ui.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lc9th5.gym.GymApplication
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.remote.ApiClient
import com.lc9th5.gym.data.repository.WorkoutRepository
import com.lc9th5.gym.data.repository.TwoFactorRepository
import com.lc9th5.gym.ui.theme.*
import com.lc9th5.gym.viewmodel.GuidanceViewModel
import com.lc9th5.gym.viewmodel.GuidanceViewModelFactory
import com.lc9th5.gym.viewmodel.WorkoutViewModel
import com.lc9th5.gym.viewmodel.TwoFactorViewModel
import com.lc9th5.gym.viewmodel.ChatViewModel

data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val user = tokenManager.getUser()
    val application = context.applicationContext as GymApplication
    val guidanceViewModelFactory = remember(tokenManager) { GuidanceViewModelFactory(tokenManager, application.supabaseStorageHelper) }
    val guidanceViewModel: GuidanceViewModel = viewModel(factory = guidanceViewModelFactory)
    
    // Workout ViewModel
    val apiClient = remember { ApiClient.getInstance(tokenManager) }
    val workoutRepository = remember { WorkoutRepository(apiClient.workoutApiService) }
    val workoutViewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory(workoutRepository))
    
    // Chat ViewModel
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(apiClient.chatApiService))
    var showChatDialog by remember { mutableStateOf(false) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    
    val navItems = listOf(
        NavItem("Hướng dẫn", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
        NavItem("Luyện tập", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
        NavItem("Kế hoạch", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem("Lịch", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        NavItem("Hồ sơ", Icons.Filled.Person, Icons.Outlined.Person)
    )
    
    // Collect workout UI state for reminders
    val workoutUiState by workoutViewModel.uiState.collectAsState()
    
    // Load reminders on init
    LaunchedEffect(Unit) {
        workoutViewModel.loadReminders()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                ModernTopBar(
                    title = navItems[selectedTab].label,
                    userName = user?.fullName ?: user?.username ?: "Gym User",
                    reminders = workoutUiState.reminders,
                    onLogout = {
                        tokenManager.clearTokens()
                        onLogout()
                    }
                )
            },
            bottomBar = {
                ModernNavigationBar(
                    items = navItems,
                    selectedIndex = selectedTab,
                    onItemSelected = { selectedTab = it }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            // ===== OVERDRAW FIX: Removed Box wrapper =====
            // Truyền innerPadding trực tiếp vào các screen thay vì wrapper Box
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    0 -> GuidanceScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        viewModel = guidanceViewModel,
                        isAdmin = user?.isAdmin() == true
                    )
                    1 -> WorkoutScreen(
                        viewModel = workoutViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                    )
                    2 -> WorkoutPlanScreen(
                        viewModel = workoutViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        onStartWorkout = { selectedTab = 1 }
                    )
                    3 -> StatisticsScreen(
                        viewModel = workoutViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                    )
                    4 -> ModernProfileTab(
                        username = user?.fullName ?: user?.username ?: "Unknown",
                        email = user?.email ?: "Unknown",
                        roles = user?.roles?.joinToString(", ") { it.name } ?: "USER",
                        isVerified = user?.isVerified ?: false,
                        onLogout = {
                            tokenManager.clearTokens()
                            onLogout()
                        },
                        onNavigateToAdmin = onNavigateToAdmin,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
        
        // Floating Chat Button - Góc trên bên phải, có thể kéo thả
        DraggableChatButton(
            onClick = { showChatDialog = true }
        )
        
        // Chat Dialog
        if (showChatDialog) {
            ChatDialog(
                viewModel = chatViewModel,
                onDismiss = { showChatDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    title: String,
    userName: String,
    reminders: List<com.lc9th5.gym.data.model.WorkoutReminder> = emptyList(),
    onLogout: () -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    var showNotificationDropdown by remember { mutableStateOf(false) }
    
    // Filter reminders from last 7 days
    val recentReminders = remember(reminders) {
        val now = java.time.LocalDateTime.now()
        val weekAgo = now.minusDays(7)
        reminders.filter { it.isActive }
    }
    
    // ===== OVERDRAW FIX: Removed Surface wrapper with Color.Transparent =====
    // OLD CODE (causes overdraw - Surface + Box both draw):
    // Surface(
    //     modifier = Modifier.fillMaxWidth(),
    //     color = Color.Transparent
    // ) {
    //     Box(
    //         modifier = Modifier
    //             .fillMaxWidth()
    //             .background(
    //                 brush = Brush.verticalGradient(
    //                     colors = listOf(
    //                         PrimaryLight,
    //                         PrimaryLight.copy(alpha = 0.9f)
    //                     )
    //                 )
    //             )
    //             .padding(top = 40.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
    //     ) {
    // NEW CODE (single draw layer):
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryLight,
                        PrimaryLight.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(top = 40.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Xin chào, $userName 👋",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Notification Button
                    Box {
                        IconButton(
                            onClick = { showNotificationDropdown = !showNotificationDropdown },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            BadgedBox(
                                badge = {
                                    if (recentReminders.isNotEmpty()) {
                                        Badge(
                                            containerColor = Color.Red
                                        ) {
                                            Text(
                                                text = recentReminders.size.toString(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Thông báo",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        // Notification Dropdown
                        DropdownMenu(
                            expanded = showNotificationDropdown,
                            onDismissRequest = { showNotificationDropdown = false },
                            modifier = Modifier
                                .widthIn(min = 280.dp, max = 320.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                text = "🔔 Thông báo nhắc nhở",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                            
                            if (recentReminders.isEmpty()) {
                                Text(
                                    text = "Không có thông báo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                recentReminders.take(5).forEach { reminder ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = reminder.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "⏰ ${reminder.reminderTime} - ${reminder.daysOfWeek.joinToString(", ") { it.name.take(3) }}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = { showNotificationDropdown = false },
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
                    
                    // Logout Button
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Đăng xuất",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    // ===== END OVERDRAW FIX =====
}

@Composable
fun ModernNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
    
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        items.forEachIndexed { index, item ->
            val selected = selectedIndex == index
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(18.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                selected = selected,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                    else Color.Transparent
                )
            )
        }
    }
}

@Composable
fun ModernProfileTab(
    username: String,
    email: String,
    roles: String,
    isVerified: Boolean,
    onLogout: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiClient = remember { ApiClient.getInstance(tokenManager) }
    val twoFactorRepository = remember { TwoFactorRepository(apiClient.authApiService) }
    val twoFactorViewModel: TwoFactorViewModel = viewModel(
        factory = TwoFactorViewModel.Factory(twoFactorRepository, tokenManager)
    )
    
    // State for dialogs
    var show2FADialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    
    // Get 2FA status from ViewModel
    val twoFactorUiState by twoFactorViewModel.uiState.collectAsState()
    val is2faEnabled = twoFactorUiState.is2faEnabled
    
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ===== OVERDRAW NOTE: Card uses alpha 0.3f for subtle effect, minimal overdraw impact =====
        // Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryLight, SecondaryLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = username.firstOrNull()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Verification Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isVerified) SuccessContainerLight else MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isVerified) SuccessLight else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isVerified) "Đã xác thực" else "Chưa xác thực",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isVerified) SuccessLight else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileInfoRow(
                    icon = Icons.Default.Person,
                    label = "Tên người dùng",
                    value = username
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = email
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                ProfileInfoRow(
                    icon = Icons.Default.Badge,
                    label = "Vai trò",
                    value = roles
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                SettingsItem(
                    icon = Icons.Default.Edit,
                    title = "Chỉnh sửa hồ sơ",
                    subtitle = "Cập nhật thông tin cá nhân",
                    onClick = { showEditProfileDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Đổi mật khẩu",
                    subtitle = "Bảo mật tài khoản",
                    onClick = { showChangePasswordDialog = true }
                )
                // 2FA Settings
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Xác thực 2 lớp (2FA)",
                    subtitle = "Bảo vệ tài khoản với Google Authenticator",
                    onClick = { show2FADialog = true }
                )
                if (roles.contains("ADMIN")) {
                    SettingsItem(
                        icon = Icons.Default.Settings,
                        title = "Admin Dashboard",
                        subtitle = "Quản lý hệ thống",
                        onClick = onNavigateToAdmin
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        FilledTonalButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng xuất", fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // 2FA Full Screen Dialog
    if (show2FADialog) {
        TwoFactorSettingsDialog(
            viewModel = twoFactorViewModel,
            onDismiss = { show2FADialog = false }
        )
    }
    
    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentFullName = username,
            currentUsername = tokenManager.getUsername() ?: "",
            apiClient = apiClient,
            onDismiss = { showEditProfileDialog = false },
            onSuccess = { 
                showEditProfileDialog = false
                // Reload user info
            }
        )
    }
    
    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            is2faEnabled = is2faEnabled,
            apiClient = apiClient,
            onDismiss = { showChangePasswordDialog = false },
            onSuccess = { showChangePasswordDialog = false }
        )
    }
}

@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ===== OVERDRAW FIX: Removed Surface wrapper with Transparent color =====
// OLD: Surface(onClick=..., color=Transparent) { Row(...) }
// NEW: Row with clickable modifier directly (no extra Transparent layer)
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Keep the old ProfileTab for backward compatibility
@Composable
fun ProfileTab(
    username: String,
    email: String,
    roles: String,
    isVerified: Boolean
) {
    ModernProfileTab(
        username = username,
        email = email,
        roles = roles,
        isVerified = isVerified,
        onLogout = {},
        onNavigateToAdmin = {}
    )
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Full-screen dialog để hiển thị 2FA settings
 */
@Composable
fun TwoFactorSettingsDialog(
    viewModel: TwoFactorViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TwoFactorSettingsScreen(
                viewModel = viewModel,
                onBack = onDismiss
            )
        }
    }
}

/**
 * Dialog chỉnh sửa hồ sơ
 */
@Composable
fun EditProfileDialog(
    currentFullName: String,
    currentUsername: String,
    apiClient: ApiClient,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf(currentFullName) }
    var username by remember { mutableStateOf(currentUsername) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Chỉnh sửa hồ sơ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; error = null },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    label = { Text("Tên người dùng") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (successMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = successMessage!!,
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Hủy")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val response = apiClient.authApiService.updateProfile(
                                        com.lc9th5.gym.data.remote.UpdateProfileRequest(
                                            fullName = fullName.takeIf { it != currentFullName },
                                            username = username.takeIf { it != currentUsername }
                                        )
                                    )
                                    if (response.isSuccessful) {
                                        successMessage = "Cập nhật thành công!"
                                        kotlinx.coroutines.delay(1000)
                                        onSuccess()
                                    } else {
                                        val body = response.errorBody()?.string()
                                        error = body?.let {
                                            try {
                                                com.google.gson.Gson().fromJson(it, Map::class.java)["error"] as? String
                                            } catch (e: Exception) { null }
                                        } ?: "Cập nhật thất bại"
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Lỗi kết nối"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && (fullName != currentFullName || username != currentUsername),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Lưu")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog đổi mật khẩu
 */
@Composable
fun ChangePasswordDialog(
    is2faEnabled: Boolean,
    apiClient: ApiClient,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var twoFactorCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var show2faField by remember { mutableStateOf(is2faEnabled) }
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Đổi mật khẩu",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; error = null },
                    label = { Text("Mật khẩu hiện tại") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = { Text("Mật khẩu mới") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("Xác nhận mật khẩu mới") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                if (show2faField) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yêu cầu mã xác thực 2FA",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = twoFactorCode,
                        onValueChange = { twoFactorCode = it; error = null },
                        label = { Text("Mã 2FA từ Authenticator") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
                
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                successMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validation
                    if (newPassword != confirmPassword) {
                        error = "Mật khẩu mới không khớp"
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        error = "Mật khẩu mới phải có ít nhất 6 ký tự"
                        return@Button
                    }
                    
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            val response = apiClient.authApiService.changePassword(
                                com.lc9th5.gym.data.remote.ChangePasswordRequest(
                                    currentPassword = currentPassword,
                                    newPassword = newPassword,
                                    twoFactorCode = twoFactorCode.takeIf { it.isNotBlank() }
                                )
                            )
                            if (response.isSuccessful) {
                                successMessage = "Đổi mật khẩu thành công!"
                                kotlinx.coroutines.delay(1000)
                                onSuccess()
                            } else {
                                val body = response.errorBody()?.string()
                                val parsedBody = body?.let {
                                    try {
                                        com.google.gson.Gson().fromJson(it, Map::class.java)
                                    } catch (e: Exception) { null }
                                }
                                
                                // Check if 2FA is required
                                if (parsedBody?.get("requires2fa") == true) {
                                    show2faField = true
                                    error = parsedBody["error"] as? String ?: "Yêu cầu mã xác thực 2FA"
                                } else {
                                    error = parsedBody?.get("error") as? String ?: "Đổi mật khẩu thất bại"
                                }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Lỗi kết nối"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Đổi mật khẩu")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Hủy")
            }
        }
    )
}

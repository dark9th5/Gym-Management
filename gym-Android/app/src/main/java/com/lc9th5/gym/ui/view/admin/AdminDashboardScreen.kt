package com.lc9th5.gym.ui.view.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lc9th5.gym.data.model.AdminDashboardStats
import com.lc9th5.gym.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val stats by adminViewModel.dashboardStats.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        adminViewModel.loadDashboardStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("admin_users") }) {
                        Icon(Icons.Default.Person, contentDescription = "Manage Users")
                    }
                    IconButton(onClick = { navController.navigate("admin_notifications") }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Send Notification")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading && stats == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                DashboardContent(stats)
            }
        }
    }
}

@Composable
fun DashboardContent(stats: AdminDashboardStats?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (stats == null) {
            Text("Không có dữ liệu thống kê")
            return
        }

        StatCard(
            title = "Tổng Người Dùng",
            value = stats.totalUsers.toString(),
            icon = Icons.Default.Group,
            color = Color(0xFF4CAF50)
        )

        StatCard(
            title = "Người Dùng Mới (Hôm nay)",
            value = stats.newUsersToday.toString(),
            icon = Icons.Default.PersonAdd,
            color = Color(0xFF2196F3)
        )

        StatCard(
            title = "Tổng Bài Tập",
            value = stats.totalExercises.toString(),
            icon = Icons.Default.FitnessCenter,
            color = Color(0xFFFF9800)
        )

        StatCard(
            title = "Tổng Buổi Tập Đã Log",
            value = stats.totalWorkoutsLogged.toString(),
            icon = Icons.Default.History,
            color = Color(0xFF9C27B0)
        )
    }
}


@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

package com.lc9th5.gym.ui.view.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lc9th5.gym.data.model.UserSummary
import com.lc9th5.gym.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserListScreen(
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val users by adminViewModel.users.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        adminViewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản Lý Người Dùng") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    UserItem(user, onLockToggle = { adminViewModel.toggleUserLock(user) })
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun UserItem(user: UserSummary, onLockToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (user.isLocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.email, style = MaterialTheme.typography.titleMedium)
                user.fullName?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = if (user.isLocked) "ĐANG KHÓA" else "Hoạt động",
                    color = if (user.isLocked) MaterialTheme.colorScheme.error else Color.Green,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            IconButton(onClick = onLockToggle) {
                Icon(
                    imageVector = if (user.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = if (user.isLocked) "Mở khóa" else "Khóa",
                    tint = if (user.isLocked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

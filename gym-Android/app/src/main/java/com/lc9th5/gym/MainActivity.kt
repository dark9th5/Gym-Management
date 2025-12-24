package com.lc9th5.gym

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.ui.theme.GymTheme
import com.lc9th5.gym.ui.view.HomeScreen
import com.lc9th5.gym.ui.view.LoginScreen
import com.lc9th5.gym.ui.view.RegisterScreen

class MainActivity : ComponentActivity() {
    
    // Permission launcher for notifications (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // User denied notification permission - reminders won't work
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            GymTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    // Check if user should auto login
    val startDestination = if (tokenManager.isAutoLoginEnabled() && tokenManager.isLoggedIn()) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        // Clear back stack to prevent going back to login
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        composable("home") {
            HomeScreen(
                onLogout = {
                    navController.navigate("login") {
                        // Clear back stack when logging out
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
           composable(
               "register?verifiedEmail={verifiedEmail}",
               arguments = listOf(navArgument("verifiedEmail") {
                   type = NavType.StringType
                   defaultValue = ""
                   nullable = true
               })
           ) { backStackEntry ->
               val verifiedEmail = backStackEntry.arguments?.getString("verifiedEmail") ?: ""
               RegisterScreen(
                   verifiedEmail = verifiedEmail,
                   onRegisterSuccess = { email ->
                       navController.navigate("login")
                   },
                   onNavigateToLogin = {
                       navController.popBackStack()
                   }
               )
           }
    }
}

package com.lc9th5.gym.ui.view

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lc9th5.gym.GymApplication
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.repository.AuthRepository
import com.lc9th5.gym.ui.component.GradientButton
import com.lc9th5.gym.ui.component.ModernTextField
import com.lc9th5.gym.ui.theme.*
import com.lc9th5.gym.util.Validator
import com.lc9th5.gym.viewmodel.LoginState
import com.lc9th5.gym.viewmodel.LoginViewModel

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val repository = remember { AuthRepository((context.applicationContext as GymApplication).apiClient.authApiService) }
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(repository, context))
    val loginState by viewModel.loginState.collectAsState()
    val tokenManager = remember { TokenManager(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var autoLogin by remember { mutableStateOf(tokenManager.isAutoLoginEnabled()) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // 2FA code
    var twoFACode by remember { mutableStateOf("") }

    val isLoading by remember { derivedStateOf { loginState is LoginState.Loading } }
    
    // Check if we need to show 2FA screen
    val show2FAScreen = loginState is LoginState.Requires2FA || loginState is LoginState.TwoFAError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background decoration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PrimaryLight,
                            PrimaryLight.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        AnimatedContent(
            targetState = show2FAScreen,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith 
                slideOutHorizontally { -it } + fadeOut()
            },
            label = "login_2fa_transition"
        ) { is2FAScreen ->
            if (is2FAScreen) {
                // 2FA Verification Screen
                TwoFAVerificationContent(
                    message = when (val state = loginState) {
                        is LoginState.Requires2FA -> state.message
                        is LoginState.TwoFAError -> state.message
                        else -> ""
                    },
                    error = if (loginState is LoginState.TwoFAError) 
                        (loginState as LoginState.TwoFAError).message else null,
                    code = twoFACode,
                    onCodeChange = { if (it.length <= 8) twoFACode = it },
                    onVerify = { viewModel.verify2FA(twoFACode) },
                    onCancel = { 
                        twoFACode = ""
                        viewModel.cancel2FA()
                    },
                    isLoading = isLoading
                )
            } else {
                // Regular Login Screen
                LoginContent(
                    username = username,
                    onUsernameChange = { username = it; usernameError = null },
                    password = password,
                    onPasswordChange = { password = it; passwordError = null },
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                    autoLogin = autoLogin,
                    onAutoLoginChange = { autoLogin = it },
                    usernameError = usernameError,
                    passwordError = passwordError,
                    loginState = loginState,
                    isLoading = isLoading,
                    onLogin = {
                        usernameError = Validator.getUsernameError(username)
                        passwordError = if (password.isBlank()) "Mật khẩu không được để trống" else null
                        
                        if (usernameError == null && passwordError == null) {
                            tokenManager.setAutoLogin(autoLogin)
                            viewModel.login(username, password)
                        }
                    },
                    onNavigateToRegister = onNavigateToRegister,
                    focusManager = focusManager
                )
            }
        }
    }
    
    // Handle success
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }
}

@Composable
private fun LoginContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    autoLogin: Boolean,
    onAutoLoginChange: (Boolean) -> Unit,
    usernameError: String?,
    passwordError: String?,
    loginState: LoginState,
    isLoading: Boolean,
    onLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // App Logo/Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = "Gym Logo",
                tint = PrimaryLight,
                modifier = Modifier.size(56.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Welcome Text
        Text(
            text = "Chào mừng trở lại!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = "Đăng nhập để tiếp tục hành trình",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Login Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đăng nhập",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Username Field
                ModernTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = "Tên đăng nhập",
                    leadingIcon = Icons.Default.Person,
                    isError = usernameError != null,
                    errorMessage = usernameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Password Field
                ModernTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Mật khẩu",
                    leadingIcon = Icons.Default.Lock,
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = onPasswordVisibilityChange,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Auto Login Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoLogin,
                        onCheckedChange = onAutoLoginChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = "Ghi nhớ đăng nhập",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Login Button
                GradientButton(
                    text = "Đăng nhập",
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                    icon = Icons.Default.Login
                )
                
                // Error Message
                AnimatedVisibility(
                    visible = loginState is LoginState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (loginState as? LoginState.Error)?.message ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Register Link
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chưa có tài khoản?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onNavigateToRegister) {
                Text(
                    text = "Đăng ký ngay",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TwoFAVerificationContent(
    message: String,
    error: String?,
    code: String,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // Lock Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "2FA",
                tint = PrimaryLight,
                modifier = Modifier.size(56.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Xác thực 2 lớp",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 2FA Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = PrimaryLight,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Mở ứng dụng Authenticator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Nhập mã 6 số hoặc backup code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Code Input
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    placeholder = { 
                        Text(
                            "000000",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryLight,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                
                // Error
                AnimatedVisibility(
                    visible = error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Verify Button
                GradientButton(
                    text = "Xác nhận",
                    onClick = onVerify,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                    icon = Icons.Default.Check,
                    enabled = code.length >= 6
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Cancel Button
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quay lại đăng nhập")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Help text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mã xác thực thay đổi mỗi 30 giây. Nếu mất điện thoại, bạn có thể sử dụng backup code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

class LoginViewModelFactory(
    private val repository: AuthRepository, 
    private val context: android.content.Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

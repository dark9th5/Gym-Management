package com.lc9th5.gym.ui.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lc9th5.gym.GymApplication
import com.lc9th5.gym.data.repository.AuthRepository
import com.lc9th5.gym.ui.theme.*
import com.lc9th5.gym.util.Validator
import com.lc9th5.gym.viewmodel.EmailVerifyState
import com.lc9th5.gym.viewmodel.RegisterState
import com.lc9th5.gym.viewmodel.RegisterViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    verifiedEmail: String = "",
    onRegisterSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val appContext = LocalContext.current
    val repository = remember { AuthRepository((appContext.applicationContext as GymApplication).apiClient.authApiService) }
    val viewModel: RegisterViewModel = viewModel(factory = RegisterViewModelFactory(repository))
    val registerState by viewModel.registerState.collectAsState()
    val scrollState = rememberScrollState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var emailVerified by remember { mutableStateOf(false) }
    var emailSendMessage by remember { mutableStateOf<String?>(null) }
    val emailVerifyState by viewModel.emailVerifyState.collectAsState()

    var remainingTime by remember { mutableStateOf(0) }
    var isResendEnabled by remember { mutableStateOf(true) }

    val canSendCode by remember { derivedStateOf { isResendEnabled && emailVerifyState !is EmailVerifyState.Loading } }
    val sendCodeLabel by remember {
        derivedStateOf {
            if (emailVerifyState is EmailVerifyState.Loading) "Đang gửi..." else "Gửi mã"
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    LaunchedEffect(remainingTime) {
        if (remainingTime > 0) {
            delay(1000)
            remainingTime--
            if (remainingTime == 0) isResendEnabled = true
        }
    }

    LaunchedEffect(email, verifiedEmail) {
        if (verifiedEmail.isNotBlank() && email == verifiedEmail) emailVerified = true
    }

    val focusManager = LocalFocusManager.current
    LaunchedEffect(registerState) {
        if (registerState is RegisterState.Success) {
            android.widget.Toast.makeText(appContext, "Đăng ký thành công!", android.widget.Toast.LENGTH_LONG).show()
            onRegisterSuccess(email)
        }
    }

    LaunchedEffect(emailVerifyState) {
        when (emailVerifyState) {
            is EmailVerifyState.Success -> emailSendMessage = (emailVerifyState as EmailVerifyState.Success).message
            is EmailVerifyState.Error -> emailSendMessage = (emailVerifyState as EmailVerifyState.Error).message
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryOrange.copy(alpha = 0.1f), BackgroundLight, SecondaryOrange.copy(alpha = 0.05f))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(500)) + slideInHorizontally(initialOffsetX = { -100 })) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onNavigateToLogin, modifier = Modifier.shadow(4.dp, CircleShape).background(Color.White, CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = PrimaryOrange)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -50 })) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).shadow(8.dp, CircleShape).background(Brush.linearGradient(listOf(PrimaryOrange, SecondaryOrange)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PersonAdd, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tạo tài khoản", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Bắt đầu hành trình fitness của bạn", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(800)) + slideInVertically(initialOffsetY = { 100 })) {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RegisterTextField(fullName, { fullName = it; fullNameError = null }, "Họ và tên", Icons.Outlined.Person, fullNameError)
                        RegisterTextField(username, { username = it; usernameError = null }, "Tên đăng nhập", Icons.Outlined.AccountCircle, usernameError)
                        
                        Text("Xác thực Email", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                        
                        RegisterTextField(
                            email, { email = it; emailError = null }, "Địa chỉ email", Icons.Outlined.Email, emailError,
                            trailingIcon = if (emailVerified) {{ Icon(Icons.Filled.CheckCircle, "Đã xác thực", tint = SuccessGreen) }} else null
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.weight(1f)) {
                                RegisterTextField(code, { code = it; codeError = null }, "Mã xác thực", Icons.Outlined.Pin, codeError)
                            }
                            Button(
                                onClick = {
                                    val cleanedEmail = email.trim().lowercase()
                                    val error = Validator.getEmailError(cleanedEmail)
                                    if (error != null) { emailError = error; return@Button }
                                    if (!isResendEnabled) return@Button
                                    remainingTime = 30; isResendEnabled = false; emailSendMessage = null
                                    viewModel.requestEmailVerification(cleanedEmail)
                                },
                                enabled = canSendCode,
                                modifier = Modifier.padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, disabledContainerColor = PrimaryOrange.copy(alpha = 0.5f))
                            ) {
                                Text(if (remainingTime > 0) "${remainingTime}s" else sendCodeLabel, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        if (!emailVerified && code.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val emailErr = Validator.getEmailError(email)
                                    if (emailErr != null) { emailError = emailErr; return@OutlinedButton }
                                    if (code.isBlank()) { codeError = "Vui lòng nhập mã"; return@OutlinedButton }
                                    viewModel.verifyEmailCode(email, code)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                            ) {
                                Icon(Icons.Filled.Verified, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Xác thực mã")
                            }
                        }
                        
                        AnimatedVisibility(visible = emailVerified) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Email đã được xác thực", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        
                        emailSendMessage?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = if (emailVerifyState is EmailVerifyState.Error) ErrorRed else SuccessGreen, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerLight)
                        
                        Text("Bảo mật", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        
                        RegisterTextField(password, { password = it; passwordError = null }, "Mật khẩu", Icons.Outlined.Lock, passwordError, isPassword = true, passwordVisible = passwordVisible, onPasswordToggle = { passwordVisible = !passwordVisible })
                        RegisterTextField(confirmPassword, { confirmPassword = it; confirmPasswordError = null }, "Xác nhận mật khẩu", Icons.Outlined.LockReset, confirmPasswordError, isPassword = true, passwordVisible = confirmPasswordVisible, onPasswordToggle = { confirmPasswordVisible = !confirmPasswordVisible }, imeAction = ImeAction.Done, keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                usernameError = Validator.getUsernameError(username)
                                emailError = Validator.getEmailError(email)
                                passwordError = Validator.getPasswordError(password)
                                fullNameError = if (fullName.isBlank()) "Vui lòng nhập họ và tên" else null
                                codeError = if (code.isBlank()) "Vui lòng nhập mã xác thực" else null
                                confirmPasswordError = when {
                                    confirmPassword.isBlank() -> "Vui lòng xác nhận mật khẩu"
                                    !Validator.doPasswordsMatch(password, confirmPassword) -> "Mật khẩu không khớp"
                                    else -> null
                                }
                                if (listOf(usernameError, emailError, passwordError, confirmPasswordError, fullNameError, codeError).all { it == null }) {
                                    viewModel.register(username, email, password, fullName, code)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = registerState !is RegisterState.Loading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, disabledContainerColor = PrimaryOrange.copy(alpha = 0.6f))
                        ) {
                            if (registerState is RegisterState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.HowToReg, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tạo tài khoản", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        
                        AnimatedVisibility(visible = registerState is RegisterState.Error) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Error, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text((registerState as? RegisterState.Error)?.message ?: "", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(1000))) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("Đã có tài khoản? ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Đăng nhập", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PrimaryOrange)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = if (error != null) ErrorRed else PrimaryOrange.copy(alpha = 0.7f)) },
        trailingIcon = trailingIcon ?: if (isPassword) {{ IconButton(onClick = { onPasswordToggle?.invoke() }) { Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (passwordVisible) "Ẩn" else "Hiện", tint = TextSecondary) } }} else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        isError = error != null,
        supportingText = error?.let { { Text(it, color = ErrorRed) } },
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange, unfocusedBorderColor = DividerLight, errorBorderColor = ErrorRed, focusedLabelColor = PrimaryOrange, cursorColor = PrimaryOrange),
        modifier = modifier.fillMaxWidth()
    )
}

class RegisterViewModelFactory(private val repository: AuthRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

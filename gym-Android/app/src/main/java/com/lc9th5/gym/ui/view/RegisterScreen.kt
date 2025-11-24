package com.lc9th5.gym.ui.view

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import android.content.Context
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lc9th5.gym.viewmodel.EmailVerifyState
import com.lc9th5.gym.viewmodel.RegisterViewModel
import com.lc9th5.gym.viewmodel.RegisterState
import com.lc9th5.gym.data.repository.AuthRepository
import com.lc9th5.gym.GymApplication
import com.lc9th5.gym.util.Validator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.*

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
    // Message hiển thị kết quả gửi mã (thành công hoặc lỗi)
    var emailSendMessage by remember { mutableStateOf<String?>(null) }
    val emailVerifyState by viewModel.emailVerifyState.collectAsState()

    // Countdown timer state
    var remainingTime by remember { mutableStateOf(0) }
    var isResendEnabled by remember { mutableStateOf(true) }

    // Countdown timer effect
    LaunchedEffect(remainingTime) {
        if (remainingTime > 0) {
            delay(1000)
            remainingTime--
        } else {
            isResendEnabled = true
        }
    }

    // Nếu verifiedEmail trùng với email hiện tại, tự động xác thực
    LaunchedEffect(email, verifiedEmail) {
        if (verifiedEmail.isNotBlank() && email == verifiedEmail) {
            emailVerified = true
        }
    }

    // Handle success state
    val focusManager = LocalFocusManager.current
    LaunchedEffect(registerState) {
        if (registerState is RegisterState.Success) {
            android.widget.Toast.makeText(appContext, "Đăng ký thành công", android.widget.Toast.LENGTH_LONG).show()
            onRegisterSuccess(email)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Register", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it
                usernameError = null
            },
            label = { Text("Username") },
            supportingText = { usernameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                emailError = null
            },
            label = { Text("Email") },
            supportingText = { emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    codeError = null
                },
                label = { Text("Mã xác thực") },
                supportingText = codeError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    val cleanedEmail = email.trim().lowercase()
                    val error = Validator.getEmailError(cleanedEmail)
                    if (error != null) {
                        emailError = error
                        return@Button
                    }
                    if (!isResendEnabled) return@Button
                    // Bắt đầu countdown ngay lập tức, bất kể thành công hay lỗi
                    remainingTime = 30
                    isResendEnabled = false
                    emailSendMessage = null
                    viewModel.requestEmailVerification(cleanedEmail)
                },
                enabled = isResendEnabled && emailVerifyState !is EmailVerifyState.Loading
            ) {
                Text(if (emailVerifyState is EmailVerifyState.Loading) "Đang gửi..." else "Gửi mã")
            }
            Button(
                onClick = {
                    val emailErr = Validator.getEmailError(email)
                    if (emailErr != null) {
                        emailError = emailErr
                        return@Button
                    }
                    if (code.isBlank()) {
                        codeError = "Vui lòng nhập mã"
                        return@Button
                    }
                    viewModel.verifyEmailCode(email, code)
                },
                enabled = code.isNotBlank() && emailVerifyState !is EmailVerifyState.Loading && !emailVerified
            ) {
                Text(if (emailVerified) "Đã xác thực" else "Kiểm tra mã")
            }
        }

        // Show countdown message
        if (remainingTime > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ Vui lòng đợi ${remainingTime}s để gửi lại mã",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Lắng nghe trạng thái gửi mã & hiển thị message (không ảnh hưởng countdown đã chạy)
        LaunchedEffect(emailVerifyState) {
            when (emailVerifyState) {
                is EmailVerifyState.Success -> {
                    emailSendMessage = (emailVerifyState as EmailVerifyState.Success).message
                }
                is EmailVerifyState.Error -> {
                    emailSendMessage = (emailVerifyState as EmailVerifyState.Error).message
                }
                else -> {}
            }
        }
        // Hiển thị thông báo kết quả gửi mã phía dưới hàng nút
        emailSendMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = msg,
                color = if (emailVerifyState is EmailVerifyState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (emailVerified && email.isNotBlank()) {
            Text(
                text = "Email đã được xác thực.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { 
                fullName = it
                fullNameError = null
            },
            label = { Text("Họ và tên (Bắt buộc)") },
            supportingText = { fullNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                passwordError = null
            },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                    )
                }
            },
            supportingText = { passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { 
                confirmPassword = it
                confirmPasswordError = null
            },
            label = { Text("Confirm Password") },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                    )
                }
            },
            supportingText = { confirmPasswordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Validate all fields
                usernameError = Validator.getUsernameError(username)
                emailError = Validator.getEmailError(email)
                passwordError = Validator.getPasswordError(password)
                fullNameError = if (fullName.isBlank()) "Vui lòng nhập họ và tên" else null
                codeError = if (code.isBlank()) "Vui lòng nhập mã xác thực" else null
                confirmPasswordError = if (confirmPassword.isBlank()) "Vui lòng xác nhận mật khẩu" 
                                      else if (!Validator.doPasswordsMatch(password, confirmPassword)) "Mật khẩu không khớp" 
                                      else null
                // Only proceed if no errors
                if (usernameError == null && emailError == null && 
                    passwordError == null && confirmPasswordError == null && fullNameError == null && codeError == null) {
                    viewModel.register(username, email, password, fullName, code)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = registerState !is RegisterState.Loading
        ) {
            Text(if (registerState is RegisterState.Loading) "Đang đăng ký..." else "Đăng ký")
        }

        if (registerState is RegisterState.Error) {
            Text(
                text = (registerState as RegisterState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
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

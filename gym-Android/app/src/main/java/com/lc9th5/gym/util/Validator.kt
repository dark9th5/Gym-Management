package com.lc9th5.gym.util

import android.util.Patterns

object Validator {
    
    /**
     * Validates email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Validates username (min 3, max 50 characters)
     */
    fun isValidUsername(username: String): Boolean {
        return username.length in 3..50
    }
    
    /**
     * Validates password (min 8, max 100 characters)
     */
    fun isValidPassword(password: String): Boolean {
        return password.length in 8..100
    }
    
    /**
     * Get email validation error message
     */
    fun getEmailError(email: String): String? {
        return when {
            email.isBlank() -> "Email không được để trống"
            !isValidEmail(email) -> "Email không hợp lệ"
            else -> null
        }
    }
    
    /**
     * Get username validation error message
     */
    fun getUsernameError(username: String): String? {
        return when {
            username.isBlank() -> "Username không được để trống"
            username.length < 3 -> "Username phải có ít nhất 3 ký tự"
            username.length > 50 -> "Username không được quá 50 ký tự"
            else -> null
        }
    }
    
    /**
     * Get password validation error message
     */
    fun getPasswordError(password: String): String? {
        return when {
            password.isBlank() -> "Mật khẩu không được để trống"
            password.length < 8 -> "Mật khẩu phải có ít nhất 8 ký tự"
            password.length > 100 -> "Mật khẩu không được quá 100 ký tự"
            else -> null
        }
    }
    
    /**
     * Check if passwords match
     */
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
}

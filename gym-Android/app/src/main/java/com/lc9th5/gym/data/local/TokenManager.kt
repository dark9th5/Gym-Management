package com.lc9th5.gym.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lc9th5.gym.data.model.AuthResponse
import com.lc9th5.gym.data.model.Role
import com.lc9th5.gym.data.model.User

/**
 * Manages authentication tokens using EncryptedSharedPreferences
 */
class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "myfamily_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_USER_ROLES = "user_roles"
        private const val KEY_USER_IS_VERIFIED = "user_is_verified"
        private const val KEY_USER_USERNAME = "user_username"
        private const val KEY_USER_CREATED_AT = "user_created_at"
        private const val KEY_AUTO_LOGIN = "auto_login"
    }
    
    /**
     * Save authentication response
     */
    fun saveAuthResponse(response: AuthResponse) {
        val expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000)
        
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, response.accessToken)
            putString(KEY_REFRESH_TOKEN, response.refreshToken)
            putString(KEY_TOKEN_TYPE, response.tokenType)
            putLong(KEY_EXPIRES_AT, expiresAt)
            putLong(KEY_USER_ID, response.user.id)
            putString(KEY_USER_USERNAME, response.user.username)
            putString(KEY_USER_EMAIL, response.user.email)
            putString(KEY_USER_FULL_NAME, response.user.fullName)
            putBoolean(KEY_USER_IS_VERIFIED, response.user.isVerified)
            apply()
        }
    }
    
    /**
     * Get access token
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Get refresh token
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Get authorization header value
     */
    fun getAuthHeader(): String? {
        val token = getAccessToken()
        val tokenType = prefs.getString(KEY_TOKEN_TYPE, "Bearer")
        return if (token != null) "$tokenType $token" else null
    }
    
    /**
     * Check if token is expired
     */
    fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return System.currentTimeMillis() >= expiresAt
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null && !isTokenExpired()
    }
    
    /**
     * Get user ID
     */
    fun getUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, -1)
        return if (id != -1L) id else null
    }
    
    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Get user full name
     */
    fun getUserFullName(): String? {
        return prefs.getString(KEY_USER_FULL_NAME, null)
    }
    
    /**
     * Get username
     */
    fun getUsername(): String? {
        return prefs.getString(KEY_USER_USERNAME, null)
    }
    
    /**
     * Get user verification status
     */
    fun getUserIsVerified(): Boolean {
        return prefs.getBoolean(KEY_USER_IS_VERIFIED, false)
    }
    
    /**
     * Clear all tokens (logout)
     */
    fun clearTokens() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Update access token (after refresh)
     */
    fun updateAccessToken(accessToken: String, expiresIn: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
        
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putLong(KEY_EXPIRES_AT, expiresAt)
            apply()
        }
    }
    
    /**
     * Save full user information
     */
    fun saveUser(user: User) {
        val rolesJson = gson.toJson(user.roles)
        
        prefs.edit().apply {
            putLong(KEY_USER_ID, user.id)
            putString(KEY_USER_USERNAME, user.username)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_FULL_NAME, user.fullName)
            putString(KEY_USER_ROLES, rolesJson)
            putBoolean(KEY_USER_IS_VERIFIED, user.isVerified)
            putString(KEY_USER_CREATED_AT, user.createdAt)
            apply()
        }
    }
    
    /**
     * Get full user information
     */
    fun getUser(): User? {
        val id = getUserId() ?: return null
        val username = prefs.getString(KEY_USER_USERNAME, null) ?: return null
        val email = getUserEmail() ?: return null
        val fullName = getUserFullName()
        val rolesJson = prefs.getString(KEY_USER_ROLES, null)
        val roles = if (rolesJson != null) {
            try {
                val type = object : TypeToken<Set<Role>>() {}.type
                gson.fromJson<Set<Role>>(rolesJson, type)
            } catch (e: Exception) {
                setOf(Role.USER)
            }
        } else {
            setOf(Role.USER)
        }
        val isVerified = prefs.getBoolean(KEY_USER_IS_VERIFIED, false)
        val createdAt = prefs.getString(KEY_USER_CREATED_AT, null)
        
        return User(
            id = id,
            username = username,
            email = email,
            fullName = fullName,
            roles = roles,
            isVerified = isVerified,
            createdAt = createdAt
        )
    }
    
    /**
     * Get user roles
     */
    fun getUserRoles(): Set<Role> {
        return getUser()?.roles ?: setOf(Role.USER)
    }
    
    /**
     * Check if user is verified
     */
    fun isUserVerified(): Boolean {
        return prefs.getBoolean(KEY_USER_IS_VERIFIED, false)
    }
    
    /**
     * Check if user is admin
     */
    fun isUserAdmin(): Boolean {
        return getUserRoles().contains(Role.ADMIN)
    }
    
    /**
     * Save auto login preference
     */
    fun setAutoLogin(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
    }
    
    /**
     * Get auto login preference
     */
    fun isAutoLoginEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_LOGIN, false)
    }
}

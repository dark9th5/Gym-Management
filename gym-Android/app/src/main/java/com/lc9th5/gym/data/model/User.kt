package com.lc9th5.gym.data.model

import com.google.gson.annotations.SerializedName

/**
 * Full User model
 * Matches backend User entity (without sensitive data like password)
 */
data class User(
    val id: Long,
    
    val username: String,
    
    val email: String,
    
    @SerializedName("full_name")
    val fullName: String? = null,
    
    val roles: Set<Role> = setOf(Role.USER),
    
    @SerializedName("is_verified")
    val isVerified: Boolean = false,
    
    @SerializedName("created_at")
    val createdAt: String? = null  // ISO 8601 date string
) {
    /**
     * Check if user has admin role
     */
    fun isAdmin(): Boolean = roles.contains(Role.ADMIN)
    
    /**
     * Check if user is a regular user
     */
    fun isUser(): Boolean = roles.contains(Role.USER)
    
    /**
     * Get display name (fullName or username)
     */
    fun getDisplayName(): String = fullName ?: username
}

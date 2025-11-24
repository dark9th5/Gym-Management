package com.lc9th5.gym.data.model

import com.google.gson.annotations.SerializedName

/**
 * Family model
 */
data class Family(
    val id: Long,
    val name: String,
    val description: String? = null,
    
    @SerializedName("creatorId")
    val creatorId: Long,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * Family detail with members
 */
data class FamilyDetail(
    val id: Long,
    val name: String,
    val description: String? = null,
    
    @SerializedName("creatorId")
    val creatorId: Long,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    
    val members: List<FamilyMemberInfo>
)

/**
 * Family member information
 */
data class FamilyMemberInfo(
    val id: Long,
    val userId: Long,
    val username: String,
    val email: String,
    val fullName: String? = null,
    val role: MemberRole,
    val relationship: String? = null,
    
    @SerializedName("joinedAt")
    val joinedAt: String? = null
) {
    fun getDisplayName(): String = fullName ?: username
    fun isAdmin(): Boolean = role == MemberRole.ADMIN
}

/**
 * Member role enum
 */
enum class MemberRole {
    ADMIN,
    MEMBER
}

/**
 * Request DTOs
 */
data class CreateFamilyRequest(
    val name: String,
    val description: String? = null
)

data class UpdateFamilyRequest(
    val name: String? = null,
    val description: String? = null
)

data class AddMemberRequest(
    val userEmail: String,
    val role: String = "MEMBER",
    val relationship: String? = null
)

data class UpdateMemberRoleRequest(
    val role: String
)

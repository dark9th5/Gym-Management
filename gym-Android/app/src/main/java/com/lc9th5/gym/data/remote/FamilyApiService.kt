package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for family-related endpoints
 */
interface FamilyApiService {
    
    /**
     * Create a new family
     */
    @POST("api/family")
    suspend fun createFamily(
        @Body request: CreateFamilyRequest,
        @Header("Authorization") authHeader: String
    ): Response<Family>
    
    /**
     * Get all families for current user
     */
    @GET("api/family")
    suspend fun getUserFamilies(
        @Header("Authorization") authHeader: String
    ): Response<List<Family>>
    
    /**
     * Get family by ID with details
     */
    @GET("api/family/{id}")
    suspend fun getFamilyById(
        @Path("id") familyId: Long,
        @Header("Authorization") authHeader: String
    ): Response<FamilyDetail>
    
    /**
     * Update family
     */
    @PUT("api/family/{id}")
    suspend fun updateFamily(
        @Path("id") familyId: Long,
        @Body request: UpdateFamilyRequest,
        @Header("Authorization") authHeader: String
    ): Response<Family>
    
    /**
     * Delete family
     */
    @DELETE("api/family/{id}")
    suspend fun deleteFamily(
        @Path("id") familyId: Long,
        @Header("Authorization") authHeader: String
    ): Response<Map<String, String>>
    
    /**
     * Get family members
     */
    @GET("api/family/{id}/members")
    suspend fun getFamilyMembers(
        @Path("id") familyId: Long,
        @Header("Authorization") authHeader: String
    ): Response<List<FamilyMemberInfo>>
    
    /**
     * Add member to family
     */
    @POST("api/family/{id}/members")
    suspend fun addMember(
        @Path("id") familyId: Long,
        @Body request: AddMemberRequest,
        @Header("Authorization") authHeader: String
    ): Response<FamilyMemberInfo>
    
    /**
     * Remove member from family
     */
    @DELETE("api/family/{familyId}/members/{userId}")
    suspend fun removeMember(
        @Path("familyId") familyId: Long,
        @Path("userId") userId: Long,
        @Header("Authorization") authHeader: String
    ): Response<Map<String, String>>
    
    /**
     * Update member role
     */
    @PATCH("api/family/{familyId}/members/{userId}")
    suspend fun updateMemberRole(
        @Path("familyId") familyId: Long,
        @Path("userId") userId: Long,
        @Body request: UpdateMemberRoleRequest,
        @Header("Authorization") authHeader: String
    ): Response<FamilyMemberInfo>
}

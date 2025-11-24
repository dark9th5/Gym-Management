package com.lc9th5.gym.data.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String,
    val code: String
)

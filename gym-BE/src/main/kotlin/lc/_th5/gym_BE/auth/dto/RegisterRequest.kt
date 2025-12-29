package lc._th5.gym_BE.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 3, max = 50)
    val username: String,
    @field:Email @field:NotBlank
    val email: String,
    @field:NotBlank @field:Size(min = 8, max = 100)
    val password: String,
    @field:NotBlank
    val fullName: String,
    @field:NotBlank
    val code: String
)
package com.lilac.identity.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val isEmailVerified: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

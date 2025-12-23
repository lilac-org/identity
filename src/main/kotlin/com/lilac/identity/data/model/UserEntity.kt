package com.lilac.identity.data.model

import java.util.UUID

data class UserEntity(
    val id: UUID,
    val email: String,
    val username: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val isEmailVerified: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
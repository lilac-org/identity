package com.lilac.identity.domain.model

data class User(
    val id: String,
    val email: String,
    val username: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val isEmailVerified: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
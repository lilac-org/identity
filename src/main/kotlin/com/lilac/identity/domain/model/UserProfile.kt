package com.lilac.identity.domain.model

data class UserProfile(
    val id: String,
    val userId: String,
    val bio: String?,
    val profilePictureUrl: String?,
    val coverPictureUrl: String?,
    val createdAt: Long,
    val updatedAt: Long
)
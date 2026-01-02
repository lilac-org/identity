package com.lilac.identity.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val bio: String?,
    val profilePictureUrl: String?,
    val coverPictureUrl: String?,
    val createdAt: Long,
    val updatedAt: Long
)
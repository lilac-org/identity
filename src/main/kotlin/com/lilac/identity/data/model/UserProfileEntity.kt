package com.lilac.identity.data.model

import java.util.UUID

data class UserProfileEntity(
    val id: UUID,
    val userId: UUID,
    val bio: String?,
    val profilePictureUrl: String?,
    val coverPictureUrl: String?,
    val createdAt: Long,
    val updatedAt: Long
)
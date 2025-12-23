package com.lilac.identity.data.mapper

import com.lilac.identity.data.model.UserProfileEntity
import com.lilac.identity.domain.model.UserProfile
import com.lilac.identity.util.toUUID

fun UserProfileEntity.toDomain() = UserProfile(
    id = this.id.toString(),
    userId = this.userId.toString(),
    bio = this.bio,
    profilePictureUrl = this.profilePictureUrl,
    coverPictureUrl = this.coverPictureUrl,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = this.id.toUUID(),
    userId = this.userId.toUUID(),
    bio = this.bio,
    profilePictureUrl = this.profilePictureUrl,
    coverPictureUrl = this.coverPictureUrl,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
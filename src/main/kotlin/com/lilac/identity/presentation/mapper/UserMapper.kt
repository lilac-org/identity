package com.lilac.identity.presentation.mapper

import com.lilac.identity.domain.model.UserDetail
import com.lilac.identity.domain.model.UserProfile
import com.lilac.identity.presentation.dto.UserDetailDto
import com.lilac.identity.presentation.dto.UserProfileDto
import com.lilac.identity.presentation.dto.UserPublicDetailDto

fun UserProfile.toDto() = UserProfileDto(
    bio = this.bio,
    profilePictureUrl = this.profilePictureUrl,
    coverPictureUrl = this.coverPictureUrl,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

fun UserDetail.toDto() = UserDetailDto(
    id = this.id,
    email = this.email,
    username = this.username,
    firstName = this.firstName,
    lastName = this.lastName,
    isEmailVerified = this.isEmailVerified,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    profile = this.profile?.toDto()
)

fun UserDetail.toPublicDto() = UserPublicDetailDto(
    id = this.id,
    username = this.username,
    name = "${this.firstName} ${this.lastName}",
    bio = this.profile?.bio,
    profilePictureUrl = this.profile?.profilePictureUrl,
    coverPictureUrl = this.profile?.coverPictureUrl,
)
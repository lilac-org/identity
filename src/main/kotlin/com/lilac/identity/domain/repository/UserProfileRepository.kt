package com.lilac.identity.domain.repository

import com.lilac.identity.domain.model.UserProfile

interface UserProfileRepository {
    fun findByUserId(userId: String): UserProfile?
    fun create(
        userId: String,
        bio: String?,
        profilePictureUrl: String?,
        coverPictureUrl: String?
    ): String
    fun update(profile: UserProfile): Boolean
}
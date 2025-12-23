package com.lilac.identity.domain.service

interface JwtService {
    fun generateAccessToken(
        userId: String,
        username: String,
        firstName: String,
        lastName: String,
        isEmailVerified: Boolean
    ): String

    fun generateRefreshToken(
        userId: String
    ): String
}
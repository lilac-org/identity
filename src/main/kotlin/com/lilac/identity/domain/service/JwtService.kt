package com.lilac.identity.domain.service

import com.auth0.jwt.interfaces.DecodedJWT

interface JwtService {
    val domain: String
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

    fun generateEmailVerificationToken(
        userId: String
    ): String

    fun generatePasswordResetToken(
        userId: String
    ): String

    fun decodeEmailVerificationToken(
        token: String
    ): DecodedJWT?

    fun decodePasswordResetToken(
        token: String
    ): DecodedJWT?

    fun decodeRefreshToken(
        token: String
    ): DecodedJWT?
}
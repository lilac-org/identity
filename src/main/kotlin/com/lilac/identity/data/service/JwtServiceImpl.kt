package com.lilac.identity.data.service

import com.auth0.jwt.JWT
import com.lilac.identity.config.JwtConfig
import com.lilac.identity.domain.service.JwtService
import java.util.Date

class JwtServiceImpl(
    private val config: JwtConfig
): JwtService {
    override fun generateAccessToken(
        userId: String,
        username: String,
        firstName: String,
        lastName: String,
        isEmailVerified: Boolean
    ): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(userId)
        .withClaim("username", username)
        .withClaim("first_name", firstName)
        .withClaim("last_name", lastName)
        .withClaim("email_verified", isEmailVerified)
        .withClaim("type", "access")
        .withIssuedAt(Date(System.currentTimeMillis()))
        .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpInMillis))
        .sign(config.algorithm)

    override fun generateRefreshToken(userId: String): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(userId)
        .withIssuedAt(Date(System.currentTimeMillis()))
        .withExpiresAt(Date(System.currentTimeMillis() + config.refreshTokenExpInMillis))
        .sign(config.algorithm)
}
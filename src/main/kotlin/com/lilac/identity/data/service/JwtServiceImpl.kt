package com.lilac.identity.data.service

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.lilac.identity.config.JwtConfig
import com.lilac.identity.domain.service.JwtService
import java.util.Date

class JwtServiceImpl(
    private val config: JwtConfig
): JwtService {
    private val TYPE_ACCESS = "access"
    private val TYPE_REFRESH = "refresh"
    private val TYPE_EMAIL_VERIFICATION = "email_verification"
    private val TYPE_PASSWORD_RESET = "password_reset"

    override val domain = config.domain

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
        .withClaim("type", TYPE_ACCESS)
        .withIssuedAt(Date(System.currentTimeMillis()))
        .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpInMillis))
        .sign(config.algorithm)

    override fun generateRefreshToken(userId: String): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(userId)
        .withClaim("type", TYPE_REFRESH)
        .withIssuedAt(Date(System.currentTimeMillis()))
        .withExpiresAt(Date(System.currentTimeMillis() + config.refreshTokenExpInMillis))
        .sign(config.algorithm)

    override fun generateEmailVerificationToken(userId: String): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(userId)
        .withClaim("type", TYPE_EMAIL_VERIFICATION)
        .withIssuedAt(Date(System.currentTimeMillis()))
        .withExpiresAt(Date(System.currentTimeMillis() + config.emailVerificationExpInMillis))
        .sign(config.algorithm)

    override fun generatePasswordResetToken(userId: String): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(userId)
        .withClaim("type", TYPE_PASSWORD_RESET)
        .withIssuedAt(Date(System.currentTimeMillis()))
        .withExpiresAt(Date(System.currentTimeMillis() + config.resetPasswordExpInMillis))
        .sign(config.algorithm)

    private val verifier = JWT.require(config.algorithm)
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .build()

    private fun decodeToken(token: String, type: String): DecodedJWT? = try {
        val decoded = verifier.verify(token)
        if (decoded.getClaim("type").asString() == type)
            decoded
        else null
    } catch (e: JWTVerificationException) {
        e.printStackTrace()
        null
    }

    override fun decodeEmailVerificationToken(token: String): DecodedJWT? = decodeToken(token, TYPE_EMAIL_VERIFICATION)
    override fun decodePasswordResetToken(token: String): DecodedJWT? = decodeToken(token, TYPE_PASSWORD_RESET)
    override fun decodeRefreshToken(token: String): DecodedJWT? = decodeToken(token, TYPE_REFRESH)
}
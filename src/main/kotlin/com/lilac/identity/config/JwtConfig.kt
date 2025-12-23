package com.lilac.identity.config

import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application

data class JwtConfig(
    val domain: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    private val secret: String,
    private val accessTokenExpInMins: Long,
    private val refreshTokenExpInDays: Long
) {
    val accessTokenExpInMillis = accessTokenExpInMins * 60 * 1000
    val refreshTokenExpInMillis = refreshTokenExpInDays * 24 * 60 * 60 * 1000
    val algorithm = Algorithm.HMAC512(secret)
}

fun Application.loadJwtConfig(): JwtConfig {
    val cfg = environment.config

    return JwtConfig(
        domain = cfg.propertyOrNull("jwt.domain")?.getString() ?: error("JWT Domain must be specified"),
        realm = cfg.propertyOrNull("jwt.realm")?.getString() ?: error("JWT Realm must be specified"),
        secret = cfg.propertyOrNull("jwt.secret")?.getString() ?: error("JWT Secret must be specified"),
        audience = cfg.propertyOrNull("jwt.audience")?.getString() ?: error("JWT Audience must be specified"),
        issuer = cfg.propertyOrNull("jwt.issuer")?.getString() ?: error("JWT Issuer must be specified"),
        accessTokenExpInMins = cfg.propertyOrNull("jwt.accessTokenExpInMins")?.getString()?.toLong() ?: 10,
        refreshTokenExpInDays = cfg.propertyOrNull("jwt.refreshTokenExpInDays")?.getString()?.toLong() ?: 30
    )
}
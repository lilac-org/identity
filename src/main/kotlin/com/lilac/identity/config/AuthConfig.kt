package com.lilac.identity.config

import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

data class AuthConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    private val privateKeyPath: String,
    private val accessTokenExpInMins: Long,
    private val refreshTokenExpInDays: Long,
) {
    val accessTokenExpInMillis = accessTokenExpInMins * 60 * 1000
    val refreshTokenExpInMillis = refreshTokenExpInDays * 24 * 60 * 60 * 1000
    val algorithm: Algorithm = Algorithm.RSA256(
        null,
        loadPrivateKey(privateKeyPath)
    )
}

fun Application.loadAuthConfig(): AuthConfig {
    val cfg = environment.config

    return AuthConfig(
        realm = cfg.propertyOrNull("auth.realm")?.getString() ?: error("JWT Realm must be specified"),
        privateKeyPath = cfg.propertyOrNull("auth.privateKeyPath")?.getString() ?: error("JWT Private Key Path must be specified"),
        audience = cfg.propertyOrNull("auth.audience")?.getString() ?: error("JWT Audience must be specified"),
        issuer = cfg.propertyOrNull("auth.issuer")?.getString() ?: error("JWT Issuer must be specified"),
        accessTokenExpInMins = cfg.propertyOrNull("auth.accessTokenExpInMins")?.getString()?.toLong() ?: 10,
        refreshTokenExpInDays = cfg.propertyOrNull("auth.refreshTokenExpInDays")?.getString()?.toLong() ?: 30,
    )
}

fun loadPrivateKey(path: String): RSAPrivateKey {
    val key = Files.readString(Paths.get(path))
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\\s".toRegex(), "")

    val decoded = Base64.getDecoder().decode(key)
    val spec = PKCS8EncodedKeySpec(decoded)
    return KeyFactory.getInstance("RSA").generatePrivate(spec) as RSAPrivateKey
}
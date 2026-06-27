package id.andreasmlbngaol.identity.domain.model

import kotlin.time.Instant

/**
 * The pair of tokens returned to a client after a successful authentication or
 * refresh. The access token is a stateless JWT; the refresh token is the raw
 * (un-hashed) opaque value the caller must store and present to rotate.
 */
data class AuthTokens(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val tokenType: String = "Bearer",
)

package id.andreasmlbngaol.identity.domain.service

import id.andreasmlbngaol.identity.domain.model.AccessTokenClaims
import id.andreasmlbngaol.identity.domain.model.User
import kotlin.time.Instant

/**
 * Issues and verifies stateless access tokens (RS256 JWT) and generates the raw
 * opaque refresh-token values. The signing keys live in the data layer; the
 * matching public keys are exposed through a JWKS endpoint so any downstream
 * backend can verify tokens locally without calling this service.
 */
interface TokenIssuer {
    /** Result of minting an access token: the compact JWT plus its expiry. */
    data class IssuedAccessToken(val token: String, val expiresAt: Instant)

    /** Result of minting a service (client-credentials) token. */
    data class IssuedServiceToken(val token: String, val expiresAt: Instant)

    fun issueAccessToken(user: User, audiences: Set<String>): IssuedAccessToken

    fun issueServiceToken(clientId: String, scopes: Set<String>, audiences: Set<String>): IssuedServiceToken

    /** Generates a cryptographically random, URL-safe refresh-token value. */
    fun generateRefreshToken(): String

    /** Verifies signature/expiry/issuer and decodes the claims, or throws. */
    fun verifyAccessToken(token: String): AccessTokenClaims
}

package id.andreasmlbngaol.identity.domain.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A persisted, rotating refresh token. Only a SHA-256 hash of the raw token is
 * ever stored. Tokens belong to a "family": rotation issues a child token and
 * marks the parent used; if a used (or revoked) token is presented again, the
 * whole family is revoked (reuse detection).
 */
data class RefreshToken(
    val id: Uuid,
    val userId: Uuid,
    /** Groups all rotations originating from a single login. */
    val familyId: Uuid,
    /** SHA-256 hex of the raw refresh token. */
    val tokenHash: String,
    /** The client/application the token was issued to (audience), if any. */
    val clientId: String? = null,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant? = null,
    val revokedAt: Instant? = null,
    /** Set when this token was rotated, pointing at its replacement. */
    val replacedByTokenId: Uuid? = null,
    val userAgent: String? = null,
    val ipAddress: String? = null,
) {
    fun isActive(now: Instant): Boolean =
        revokedAt == null && usedAt == null && now < expiresAt
}

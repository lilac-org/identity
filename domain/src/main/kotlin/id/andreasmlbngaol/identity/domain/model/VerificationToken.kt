package id.andreasmlbngaol.identity.domain.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A single-use, hashed token used for email verification or password reset.
 * Only the SHA-256 hash of the raw token is stored; the raw value is delivered
 * to the user out-of-band (email).
 */
data class VerificationToken(
    val id: Uuid,
    val userId: Uuid,
    val tokenHash: String,
    val expiresAt: Instant,
    val usedAt: Instant? = null,
    val createdAt: Instant,
) {
    fun isUsable(now: Instant): Boolean = usedAt == null && now < expiresAt
}

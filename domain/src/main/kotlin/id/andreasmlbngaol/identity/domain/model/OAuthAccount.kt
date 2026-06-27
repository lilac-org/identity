package id.andreasmlbngaol.identity.domain.model

import id.andreasmlbngaol.identity.domain.enums.AuthProvider
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Links a [User] to an external identity provider account (Google, GitHub).
 * A user may link multiple providers; the pair (provider, providerUserId) is
 * unique.
 */
data class OAuthAccount(
    val id: Uuid,
    val userId: Uuid,
    val provider: AuthProvider,
    /** The stable subject id reported by the provider. */
    val providerUserId: String,
    val email: String? = null,
    val createdAt: Instant,
)

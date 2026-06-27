package id.andreasmlbngaol.identity.domain.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A registered consuming application ("client"). Every backend that relies on
 * this identity service is registered here so tokens can be targeted at it via
 * the `aud` claim, and so machine-to-machine (client-credentials) access can be
 * authorized per client.
 *
 * Only a hash of the client secret is stored.
 */
data class Client(
    val id: Uuid,
    /** Public identifier shared with the consuming app. */
    val clientId: String,
    /** Argon2id hash of the client secret. Null for public clients. */
    val clientSecretHash: String? = null,
    val name: String,
    /** Audiences this client is allowed to request in the `aud` claim. */
    val allowedAudiences: Set<String> = emptySet(),
    /** Scopes/permissions this client may request for service tokens. */
    val allowedScopes: Set<String> = emptySet(),
    /** Allowed OAuth redirect URIs (for browser-based flows). */
    val redirectUris: Set<String> = emptySet(),
    val isConfidential: Boolean = true,
    val enabled: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)

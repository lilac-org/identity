package id.andreasmlbngaol.identity.domain.model

import id.andreasmlbngaol.identity.domain.enums.TokenType
import kotlin.uuid.Uuid

/**
 * The decoded, verified claims of an access token. Produced by
 * [id.andreasmlbngaol.identity.domain.service.TokenIssuer.verifyAccessToken].
 */
data class AccessTokenClaims(
    val subject: Uuid,
    val tokenType: TokenType,
    val tokenVersion: Int,
    val roles: Set<String>,
    val permissions: Set<String>,
    val audiences: Set<String>,
    val tokenId: String,
)

package id.andreasmlbngaol.identity.domain.repository

import id.andreasmlbngaol.identity.domain.model.RefreshToken
import kotlin.uuid.Uuid

interface RefreshTokenRepository {
    suspend fun create(token: RefreshToken): RefreshToken
    suspend fun findByHash(tokenHash: String): RefreshToken?

    /** Marks a token as used and links it to the rotated replacement. */
    suspend fun markUsed(id: Uuid, replacedByTokenId: Uuid)

    suspend fun revoke(id: Uuid)

    /** Reuse-detection countermeasure: revoke an entire token family. */
    suspend fun revokeFamily(familyId: Uuid)

    /** Revoke every active token for a user ("log out everywhere"). */
    suspend fun revokeAllForUser(userId: Uuid)

    suspend fun deleteExpired(): Int
}

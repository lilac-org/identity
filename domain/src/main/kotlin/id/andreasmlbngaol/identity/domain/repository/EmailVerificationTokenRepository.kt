package id.andreasmlbngaol.identity.domain.repository

import id.andreasmlbngaol.identity.domain.model.VerificationToken
import kotlin.uuid.Uuid

interface EmailVerificationTokenRepository {
    suspend fun create(token: VerificationToken): VerificationToken
    suspend fun findByHash(tokenHash: String): VerificationToken?
    suspend fun markUsed(id: Uuid)
    /** Invalidate any outstanding tokens for a user before issuing a new one. */
    suspend fun invalidateAllForUser(userId: Uuid)
}

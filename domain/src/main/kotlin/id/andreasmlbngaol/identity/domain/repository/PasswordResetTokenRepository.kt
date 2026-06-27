package id.andreasmlbngaol.identity.domain.repository

import id.andreasmlbngaol.identity.domain.model.VerificationToken
import kotlin.uuid.Uuid

interface PasswordResetTokenRepository {
    suspend fun create(token: VerificationToken): VerificationToken
    suspend fun findByHash(tokenHash: String): VerificationToken?
    suspend fun markUsed(id: Uuid)
    suspend fun invalidateAllForUser(userId: Uuid)
}

package id.andreasmlbngaol.identity.domain.repository

import id.andreasmlbngaol.identity.domain.enums.AuthProvider
import id.andreasmlbngaol.identity.domain.model.OAuthAccount
import kotlin.uuid.Uuid

interface OAuthAccountRepository {
    suspend fun findByProvider(provider: AuthProvider, providerUserId: String): OAuthAccount?
    suspend fun findByUserId(userId: Uuid): List<OAuthAccount>
    suspend fun link(account: OAuthAccount): OAuthAccount
}

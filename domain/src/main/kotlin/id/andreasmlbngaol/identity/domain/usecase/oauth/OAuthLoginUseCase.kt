package id.andreasmlbngaol.identity.domain.usecase.oauth

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.enums.AuthProvider
import id.andreasmlbngaol.identity.domain.enums.UserStatus
import id.andreasmlbngaol.identity.domain.error.OAuthException
import id.andreasmlbngaol.identity.domain.model.AuthTokens
import id.andreasmlbngaol.identity.domain.model.OAuthAccount
import id.andreasmlbngaol.identity.domain.model.RequestContext
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.domain.repository.OAuthAccountRepository
import id.andreasmlbngaol.identity.domain.repository.RoleRepository
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import id.andreasmlbngaol.identity.domain.service.OAuthProvider
import id.andreasmlbngaol.identity.domain.service.OAuthUserProfile
import id.andreasmlbngaol.identity.domain.service.TransactionRunner
import id.andreasmlbngaol.identity.domain.usecase.auth.LoginUseCase

/**
 * Begins an OAuth flow (returns the provider's authorization URL) and completes
 * it (exchanges the code, links or creates the local user, then issues tokens).
 * New accounts created via OAuth are ACTIVE and email-verified when the provider
 * reports a verified email.
 */
class OAuthLoginUseCase(
    private val providers: Map<AuthProvider, OAuthProvider>,
    private val users: UserRepository,
    private val oauthAccounts: OAuthAccountRepository,
    private val roles: RoleRepository,
    private val audit: AuditLogger,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val transaction: TransactionRunner,
    private val loginUseCase: LoginUseCase,
) {
    fun authorizationUrl(provider: AuthProvider, state: String, redirectUri: String): String {
        val p = providers[provider] ?: throw OAuthException("Unsupported provider: $provider")
        return p.authorizationUrl(state, redirectUri)
    }

    suspend fun callback(
        provider: AuthProvider,
        code: String,
        redirectUri: String,
        audiences: Set<String>,
        ctx: RequestContext,
    ): AuthTokens {
        val p = providers[provider] ?: throw OAuthException("Unsupported provider: $provider")
        val profile = p.exchangeCode(code, redirectUri)
        val user = resolveUser(provider, profile)
        audit.record(AuditAction.OAUTH_LOGIN, user.id, ctx, metadata = mapOf("provider" to provider.name))
        return loginUseCase.issueTokens(user, audiences, ctx)
    }

    private suspend fun resolveUser(provider: AuthProvider, profile: OAuthUserProfile): User {
        oauthAccounts.findByProvider(provider, profile.providerUserId)?.let { existing ->
            return users.findById(existing.userId) ?: throw OAuthException("Linked user not found")
        }
        val now = clock.now()
        val email = profile.email?.trim()?.lowercase()

        // Link to an existing local account that owns the same (verified) email.
        val existingByEmail = email?.let { users.findByEmail(it) }
        if (existingByEmail != null) {
            oauthAccounts.link(
                OAuthAccount(idGenerator.newId(), existingByEmail.id, provider, profile.providerUserId, email, now),
            )
            audit.record(AuditAction.OAUTH_ACCOUNT_LINKED, existingByEmail.id)
            return existingByEmail
        }

        // Otherwise provision a brand new user.
        val defaultRole = roles.findByName("USER")
        val username = generateUsername(email, profile.providerUserId)
        val newUser = User(
            id = idGenerator.newId(),
            email = email ?: "$username@oauth.local",
            username = username,
            passwordHash = null,
            status = UserStatus.ACTIVE,
            emailVerified = profile.emailVerified,
            fullName = profile.fullName,
            photoUrl = profile.photoUrl,
            roles = defaultRole?.let { setOf(it) } ?: emptySet(),
            createdAt = now,
            updatedAt = now,
        )
        return transaction.inTransaction {
            val created = users.create(newUser)
            defaultRole?.let { roles.assignRoleToUser(created.id, it.id) }
            oauthAccounts.link(
                OAuthAccount(idGenerator.newId(), created.id, provider, profile.providerUserId, email, now),
            )
            created
        }
    }

    private suspend fun generateUsername(email: String?, providerUserId: String): String {
        val base = (email?.substringBefore('@') ?: "user")
            .lowercase().replace(Regex("[^a-z0-9_.]"), "").take(24).ifBlank { "user" }
        var candidate = base
        var suffix = 0
        while (users.existsByUsername(candidate)) {
            suffix++
            candidate = (base.take(24) + suffix)
        }
        return candidate
    }
}

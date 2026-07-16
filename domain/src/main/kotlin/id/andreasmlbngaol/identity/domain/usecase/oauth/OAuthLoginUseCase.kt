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
import id.andreasmlbngaol.identity.domain.repository.ClientRepository
import id.andreasmlbngaol.identity.domain.error.ClientException
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import id.andreasmlbngaol.identity.domain.service.OAuthProvider
import id.andreasmlbngaol.identity.domain.service.OAuthUserProfile
import id.andreasmlbngaol.identity.domain.service.TransactionRunner
import id.andreasmlbngaol.identity.domain.usecase.auth.LoginUseCase
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

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
    private val clients: ClientRepository,
) {
    private data class PendingAuthorization(
        val provider: AuthProvider,
        val clientId: String,
        val redirectUri: String,
        val clientState: String,
        val codeChallenge: String,
        val expiresAt: kotlin.time.Instant,
    )

    private data class AuthorizationCode(
        val user: User,
        val clientId: String,
        val redirectUri: String,
        val codeChallenge: String,
        val expiresAt: kotlin.time.Instant,
    )

    data class CallbackResult(
        val redirectUri: String,
        val code: String,
        val state: String,
    )

    private val pendingAuthorizations = ConcurrentHashMap<String, PendingAuthorization>()
    private val authorizationCodes = ConcurrentHashMap<String, AuthorizationCode>()

    suspend fun beginAuthorization(
        provider: AuthProvider,
        clientId: String,
        redirectUri: String,
        state: String,
        codeChallenge: String,
        identityCallbackUri: String,
    ): String {
        val client = clients.findByClientId(clientId)?.takeIf { it.enabled }
            ?: throw ClientException("Unknown or disabled client")
        if (client.isConfidential || redirectUri !in client.redirectUris) {
            throw ClientException("Invalid OAuth client or redirect URI")
        }
        if (state.isBlank() || codeChallenge.length !in 43..128 || !codeChallenge.matches(Regex("[A-Za-z0-9_-]+"))) {
            throw ClientException("OAuth requires state and an S256 PKCE code challenge")
        }
        val internalState = loginUseCase.generateOpaqueToken()
        pendingAuthorizations[internalState] = PendingAuthorization(
            provider, clientId, redirectUri, state, codeChallenge, clock.now().plus(10.minutes),
        )
        val p = providers[provider] ?: throw OAuthException("Unsupported provider: $provider")
        return p.authorizationUrl(internalState, identityCallbackUri)
    }

    suspend fun completeAuthorization(
        provider: AuthProvider,
        code: String,
        state: String,
        identityCallbackUri: String,
        ctx: RequestContext,
    ): CallbackResult {
        val pending = pendingAuthorizations.remove(state)
            ?.takeIf { it.provider == provider && clock.now() < it.expiresAt }
            ?: throw OAuthException("Invalid or expired OAuth state")
        val p = providers[provider] ?: throw OAuthException("Unsupported provider: $provider")
        val profile = p.exchangeCode(code, identityCallbackUri)
        val user = resolveUser(provider, profile)
        audit.record(AuditAction.OAUTH_LOGIN, user.id, ctx, metadata = mapOf("provider" to provider.name))
        val authorizationCode = loginUseCase.generateOpaqueToken()
        authorizationCodes[authorizationCode] = AuthorizationCode(
            user, pending.clientId, pending.redirectUri, pending.codeChallenge, clock.now().plus(1.minutes),
        )
        return CallbackResult(pending.redirectUri, authorizationCode, pending.clientState)
    }

    suspend fun exchangeAuthorizationCode(
        clientId: String,
        code: String,
        redirectUri: String,
        codeVerifier: String,
        ctx: RequestContext,
    ): AuthTokens {
        val authorization = authorizationCodes.remove(code)
            ?.takeIf { clock.now() < it.expiresAt }
            ?: throw OAuthException("Invalid or expired authorization code")
        if (authorization.clientId != clientId || authorization.redirectUri != redirectUri ||
            authorization.codeChallenge != pkceChallenge(codeVerifier)
        ) throw OAuthException("Invalid authorization code exchange")
        return loginUseCase.issueTokens(
            authorization.user,
            setOf(clientId),
            ctx.copy(clientId = clientId),
        )
    }

    private fun pkceChallenge(verifier: String): String {
        if (verifier.length !in 43..128 || !verifier.matches(Regex("[A-Za-z0-9-._~]+"))) {
            throw OAuthException("Invalid PKCE code verifier")
        }
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
            java.security.MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
        )
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
        val username = generateUsername(email)
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

    private suspend fun generateUsername(email: String?): String {
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

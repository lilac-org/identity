package id.andreasmlbngaol.identity.domain.usecase.auth

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.error.AccountNotActiveException
import id.andreasmlbngaol.identity.domain.error.EmailNotVerifiedException
import id.andreasmlbngaol.identity.domain.error.InvalidCredentialsException
import id.andreasmlbngaol.identity.domain.enums.UserStatus
import id.andreasmlbngaol.identity.domain.model.AuthTokens
import id.andreasmlbngaol.identity.domain.model.RefreshToken
import id.andreasmlbngaol.identity.domain.model.RequestContext
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.repository.RefreshTokenRepository
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import id.andreasmlbngaol.identity.domain.service.PasswordHasher
import id.andreasmlbngaol.identity.domain.service.SecretHasher
import id.andreasmlbngaol.identity.domain.service.TokenIssuer

/**
 * Authenticates a user by any single identifier (email / username / phone) plus
 * password, then issues a stateless access token and a persisted, rotating
 * refresh token (a fresh token family per login).
 */
class LoginUseCase(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordHasher: PasswordHasher,
    private val secretHasher: SecretHasher,
    private val tokenIssuer: TokenIssuer,
    private val audit: AuditLogger,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val policy: AuthPolicy,
) {
    data class Command(
        val identifier: String,
        val password: String,
        val audiences: Set<String> = emptySet(),
    )

    suspend fun execute(command: Command, ctx: RequestContext): AuthTokens {
        val user = users.findByAnyIdentifier(command.identifier.trim())
        // Always run a hash verification (even on missing users) to avoid leaking
        // account existence via timing differences.
        val passwordOk = if (user?.passwordHash != null) {
            passwordHasher.verify(command.password, user.passwordHash)
        } else {
            passwordHasher.verify(command.password, DUMMY_HASH)
            false
        }

        if (user == null || !passwordOk) {
            audit.record(AuditAction.USER_LOGIN_FAILED, user?.id, ctx, success = false)
            throw InvalidCredentialsException()
        }

        when (user.status) {
            UserStatus.SUSPENDED -> throw AccountNotActiveException("Account is suspended")
            UserStatus.DELETED -> throw InvalidCredentialsException()
            UserStatus.PENDING_VERIFICATION -> throw EmailNotVerifiedException()
            UserStatus.ACTIVE -> Unit
        }

        val tokens = issueTokens(user, command.audiences, ctx)
        audit.record(AuditAction.USER_LOGIN_SUCCEEDED, user.id, ctx)
        return tokens
    }

    internal suspend fun issueTokens(user: User, audiences: Set<String>, ctx: RequestContext): AuthTokens {
        val now = clock.now()
        val resolvedAudiences = audiences.ifEmpty { setOf(policy.defaultAudience) }
        val access = tokenIssuer.issueAccessToken(user, resolvedAudiences)
        val rawRefresh = tokenIssuer.generateRefreshToken()
        val familyId = idGenerator.newId()
        refreshTokens.create(
            RefreshToken(
                id = idGenerator.newId(),
                userId = user.id,
                familyId = familyId,
                tokenHash = secretHasher.hash(rawRefresh),
                clientId = ctx.clientId,
                issuedAt = now,
                expiresAt = now.plus(policy.refreshTokenTtl),
                userAgent = ctx.userAgent,
                ipAddress = ctx.ipAddress,
            ),
        )
        return AuthTokens(
            accessToken = access.token,
            accessTokenExpiresAt = access.expiresAt,
            refreshToken = rawRefresh,
            refreshTokenExpiresAt = now.plus(policy.refreshTokenTtl),
        )
    }

    companion object {
        // A pre-computed Argon2id hash of a random string, used for constant-time
        // behaviour when the user does not exist.
        private const val DUMMY_HASH =
            "\$argon2id\$v=19\$m=65536,t=3,p=1\$c29tZXNhbHRzb21lc2FsdA\$RdescudvJCsgt3ub+b+dWRWJTmaaJObG"
    }
}

package id.andreasmlbngaol.identity.domain.usecase.auth

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.domain.error.TokenException
import id.andreasmlbngaol.identity.domain.model.AuthTokens
import id.andreasmlbngaol.identity.domain.model.RefreshToken
import id.andreasmlbngaol.identity.domain.model.RequestContext
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.repository.RefreshTokenRepository
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import id.andreasmlbngaol.identity.domain.service.SecretHasher
import id.andreasmlbngaol.identity.domain.service.TokenIssuer
import id.andreasmlbngaol.identity.domain.service.TransactionRunner

/**
 * Rotating refresh with reuse detection:
 *  - A valid, unused refresh token is consumed and a replacement is issued.
 *  - Presenting an already-used/revoked token means it was likely stolen, so
 *    the entire token family is revoked and the attempt is rejected.
 */
class RefreshTokenUseCase(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val secretHasher: SecretHasher,
    private val tokenIssuer: TokenIssuer,
    private val audit: AuditLogger,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val transaction: TransactionRunner,
    private val policy: AuthPolicy,
) {
    data class Command(val refreshToken: String, val audiences: Set<String> = emptySet())

    suspend fun execute(command: Command, ctx: RequestContext): AuthTokens {
        val now = clock.now()
        val hash = secretHasher.hash(command.refreshToken)
        val stored = refreshTokens.findByHash(hash)
            ?: throw TokenException(ErrorCode.TOKEN_INVALID, "Refresh token is invalid")

        // Reuse detection: a token that was already used or revoked is a red flag.
        if (stored.usedAt != null || stored.revokedAt != null) {
            refreshTokens.revokeFamily(stored.familyId)
            audit.record(AuditAction.REFRESH_TOKEN_REUSE_DETECTED, stored.userId, ctx, success = false)
            throw TokenException(ErrorCode.TOKEN_REUSE_DETECTED, "Refresh token reuse detected; session revoked")
        }
        if (now >= stored.expiresAt) {
            throw TokenException(ErrorCode.TOKEN_EXPIRED, "Refresh token has expired")
        }

        val user = users.findById(stored.userId)
            ?: throw TokenException(ErrorCode.TOKEN_INVALID, "Refresh token is invalid")
        if (!user.isActive) throw TokenException(ErrorCode.TOKEN_INVALID, "Account is not active")

        val resolvedAudiences = command.audiences.ifEmpty { setOf(policy.defaultAudience) }
        val access = tokenIssuer.issueAccessToken(user, resolvedAudiences)
        val rawRefresh = tokenIssuer.generateRefreshToken()
        val replacementId = idGenerator.newId()

        transaction.inTransaction {
            refreshTokens.create(
                RefreshToken(
                    id = replacementId,
                    userId = user.id,
                    familyId = stored.familyId,
                    tokenHash = secretHasher.hash(rawRefresh),
                    clientId = ctx.clientId,
                    issuedAt = now,
                    expiresAt = now.plus(policy.refreshTokenTtl),
                    userAgent = ctx.userAgent,
                    ipAddress = ctx.ipAddress,
                ),
            )
            refreshTokens.markUsed(stored.id, replacementId)
        }

        audit.record(AuditAction.TOKEN_REFRESHED, user.id, ctx)
        return AuthTokens(
            accessToken = access.token,
            accessTokenExpiresAt = access.expiresAt,
            refreshToken = rawRefresh,
            refreshTokenExpiresAt = now.plus(policy.refreshTokenTtl),
        )
    }
}

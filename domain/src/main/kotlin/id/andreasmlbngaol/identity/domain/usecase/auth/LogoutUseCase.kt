package id.andreasmlbngaol.identity.domain.usecase.auth

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.model.RequestContext
import id.andreasmlbngaol.identity.domain.repository.RefreshTokenRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.SecretHasher
import kotlin.uuid.Uuid

/**
 * Logout. Revokes the presented refresh token's family. When [allDevices] is
 * set, revokes every refresh token for the user (server-side "log out
 * everywhere").
 */
class LogoutUseCase(
    private val refreshTokens: RefreshTokenRepository,
    private val secretHasher: SecretHasher,
    private val audit: AuditLogger,
) {
    data class Command(
        val userId: Uuid,
        val refreshToken: String? = null,
        val allDevices: Boolean = false,
    )

    suspend fun execute(command: Command, ctx: RequestContext) {
        if (command.allDevices) {
            refreshTokens.revokeAllForUser(command.userId)
        } else if (command.refreshToken != null) {
            val stored = refreshTokens.findByHash(secretHasher.hash(command.refreshToken))
            if (stored != null) refreshTokens.revokeFamily(stored.familyId)
        }
        audit.record(AuditAction.USER_LOGGED_OUT, command.userId, ctx)
    }
}

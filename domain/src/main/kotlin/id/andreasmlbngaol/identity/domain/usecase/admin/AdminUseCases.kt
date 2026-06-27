package id.andreasmlbngaol.identity.domain.usecase.admin

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.enums.UserStatus
import id.andreasmlbngaol.identity.domain.error.ConflictException
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.domain.error.NotFoundException
import id.andreasmlbngaol.identity.domain.model.PageRequest
import id.andreasmlbngaol.identity.domain.model.PageResult
import id.andreasmlbngaol.identity.domain.model.Role
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.domain.model.AuditLog
import id.andreasmlbngaol.identity.domain.repository.AuditLogRepository
import id.andreasmlbngaol.identity.domain.repository.RefreshTokenRepository
import id.andreasmlbngaol.identity.domain.repository.RoleRepository
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.model.RequestContext
import kotlin.uuid.Uuid

class ListUsersUseCase(private val users: UserRepository) {
    suspend fun execute(request: PageRequest): PageResult<User> = users.list(request)
}

class GetUserUseCase(private val users: UserRepository) {
    suspend fun execute(id: Uuid): User = users.findById(id) ?: throw NotFoundException("User not found")
}

class SetUserStatusUseCase(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val audit: AuditLogger,
    private val clock: Clock,
) {
    suspend fun suspend(id: Uuid, ctx: RequestContext): User = change(id, UserStatus.SUSPENDED, AuditAction.USER_SUSPENDED, ctx)
    suspend fun reactivate(id: Uuid, ctx: RequestContext): User = change(id, UserStatus.ACTIVE, AuditAction.USER_REACTIVATED, ctx)

    suspend fun softDelete(id: Uuid, ctx: RequestContext): User {
        val now = clock.now()
        val user = users.findById(id) ?: throw NotFoundException("User not found")
        val updated = user.copy(status = UserStatus.DELETED, deletedAt = now, updatedAt = now)
        users.update(updated)
        users.incrementTokenVersion(id)
        refreshTokens.revokeAllForUser(id)
        audit.record(AuditAction.USER_SOFT_DELETED, id, ctx)
        return updated
    }

    private suspend fun change(id: Uuid, status: UserStatus, action: AuditAction, ctx: RequestContext): User {
        val now = clock.now()
        val user = users.findById(id) ?: throw NotFoundException("User not found")
        val updated = user.copy(status = status, updatedAt = now)
        users.update(updated)
        if (status == UserStatus.SUSPENDED) {
            users.incrementTokenVersion(id)
            refreshTokens.revokeAllForUser(id)
        }
        audit.record(action, id, ctx)
        return updated
    }
}

class ManageUserRolesUseCase(
    private val users: UserRepository,
    private val roles: RoleRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val audit: AuditLogger,
) {
    suspend fun assign(userId: Uuid, roleName: String, ctx: RequestContext): User {
        users.findById(userId) ?: throw NotFoundException("User not found")
        val role = roles.findByName(roleName) ?: throw NotFoundException("Role not found")
        roles.assignRoleToUser(userId, role.id)
        users.incrementTokenVersion(userId)
        refreshTokens.revokeAllForUser(userId)
        audit.record(AuditAction.ROLE_ASSIGNED, userId, ctx, metadata = mapOf("role" to roleName))
        return users.findById(userId)!!
    }

    suspend fun revoke(userId: Uuid, roleName: String, ctx: RequestContext): User {
        users.findById(userId) ?: throw NotFoundException("User not found")
        val role = roles.findByName(roleName) ?: throw NotFoundException("Role not found")
        roles.revokeRoleFromUser(userId, role.id)
        users.incrementTokenVersion(userId)
        refreshTokens.revokeAllForUser(userId)
        audit.record(AuditAction.ROLE_REVOKED, userId, ctx, metadata = mapOf("role" to roleName))
        return users.findById(userId)!!
    }
}

class ListRolesUseCase(private val roles: RoleRepository) {
    suspend fun execute(): List<Role> = roles.findAll()
}

class ListAuditLogsUseCase(private val auditLogs: AuditLogRepository) {
    suspend fun execute(request: PageRequest): PageResult<AuditLog> = auditLogs.list(request)
}

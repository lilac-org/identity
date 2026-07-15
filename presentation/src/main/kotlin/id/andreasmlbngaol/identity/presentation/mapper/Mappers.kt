package id.andreasmlbngaol.identity.presentation.mapper

import id.andreasmlbngaol.identity.domain.model.AuditLog
import id.andreasmlbngaol.identity.domain.model.AuthTokens
import id.andreasmlbngaol.identity.domain.model.PageResult
import id.andreasmlbngaol.identity.domain.model.Role
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.presentation.dto.AuditLogResponse
import id.andreasmlbngaol.identity.presentation.dto.RoleResponse
import id.andreasmlbngaol.identity.presentation.dto.TokenResponse
import id.andreasmlbngaol.identity.presentation.dto.UserResponse
import id.andreasmlbngaol.identity.presentation.response.PageMeta
import id.andreasmlbngaol.identity.presentation.response.PagedData

/** Domain -> transport mappers. Domain models never cross the wire directly. */
fun User.toResponse(): UserResponse = UserResponse(
    id = id.toString(),
    email = email,
    username = username,
    status = status.name,
    emailVerified = emailVerified,
    fullName = fullName,
    photoUrl = photoUrl,
    phoneNumber = phoneNumber,
    phoneVerified = phoneVerified,
    dateOfBirth = dateOfBirth?.toString(),
    roles = roleNames.sorted(),
    permissions = permissionNames.sorted(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun AuthTokens.toResponse(includeRefreshToken: Boolean = true): TokenResponse = TokenResponse(
    accessToken = accessToken,
    refreshToken = refreshToken.takeIf { includeRefreshToken && it.isNotBlank() },
    accessTokenExpiresAt = accessTokenExpiresAt.toString(),
    refreshTokenExpiresAt = refreshTokenExpiresAt.toString().takeIf { includeRefreshToken && refreshToken.isNotBlank() },
)

fun Role.toResponse(): RoleResponse = RoleResponse(
    id = id.toString(),
    name = name,
    description = description,
    isSystem = isSystem,
    permissions = permissions.map { it.name }.sorted(),
)

fun AuditLog.toResponse(): AuditLogResponse = AuditLogResponse(
    id = id.toString(),
    action = action.name,
    userId = userId?.toString(),
    clientId = clientId,
    ipAddress = ipAddress,
    success = success,
    metadata = metadata,
    createdAt = createdAt.toString(),
)

fun <T, R> PageResult<T>.toPagedData(transform: (T) -> R): PagedData<R> = PagedData(
    items = items.map(transform),
    meta = PageMeta(page = page + 1, pageSize = size, totalItems = total, totalPages = totalPages.toLong()),
)

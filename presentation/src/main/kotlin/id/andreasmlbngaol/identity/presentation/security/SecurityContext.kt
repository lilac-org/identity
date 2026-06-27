package id.andreasmlbngaol.identity.presentation.security

import id.andreasmlbngaol.identity.domain.enums.TokenType
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.domain.error.PermissionDeniedException
import id.andreasmlbngaol.identity.domain.error.TokenException
import id.andreasmlbngaol.identity.domain.model.RequestContext
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import io.ktor.server.plugins.origin
import kotlin.uuid.Uuid

/**
 * The authenticated principal attached to a call after JWT verification.
 * Carries just enough claims for authorization without another DB lookup.
 *
 * Note: Ktor 3.x dropped the `Principal` marker interface; authentication now
 * stores/retrieves principals by type, so a plain data class is sufficient.
 */
data class AuthenticatedPrincipal(
    val userId: Uuid,
    val tokenType: TokenType,
    val roles: Set<String>,
    val permissions: Set<String>,
    val audiences: Set<String>,
    val tokenId: String,
) {
    fun hasRole(role: String): Boolean = roles.contains(role)
    fun hasPermission(permission: String): Boolean =
        permissions.contains(permission) || roles.contains("ADMIN")
}

fun ApplicationCall.requirePrincipal(): AuthenticatedPrincipal =
    principal<AuthenticatedPrincipal>()
        ?: throw TokenException(ErrorCode.TOKEN_INVALID, "Authentication required")

/** Enforces that the caller holds a specific permission (admins bypass). */
fun AuthenticatedPrincipal.requirePermission(permission: String) {
    if (!hasPermission(permission)) {
        throw PermissionDeniedException("Missing required permission: $permission")
    }
}

fun AuthenticatedPrincipal.requireRole(role: String) {
    if (!hasRole(role)) throw PermissionDeniedException("Missing required role: $role")
}

/** Builds the audit/request context from the inbound call. */
fun ApplicationCall.toRequestContext(): RequestContext = RequestContext(
    ipAddress = request.origin.remoteHost,
    userAgent = request.header("User-Agent"),
    clientId = null,
)

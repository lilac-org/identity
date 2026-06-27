package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.error.NotFoundException
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.dto.AssignRoleRequest
import id.andreasmlbngaol.identity.presentation.mapper.toPagedData
import id.andreasmlbngaol.identity.presentation.mapper.toResponse
import id.andreasmlbngaol.identity.presentation.plugin.ACCESS_AUTH
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import id.andreasmlbngaol.identity.presentation.security.requirePermission
import id.andreasmlbngaol.identity.presentation.security.requirePrincipal
import id.andreasmlbngaol.identity.presentation.security.toRequestContext
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.uuid.Uuid

/**
 * /api/v1/admin — user administration, role assignment, and audit log access.
 * Every handler enforces a fine-grained `resource:action` permission (ADMIN
 * role bypasses the individual checks).
 */
fun Route.adminRoutes(deps: ApiDependencies) {
    authenticate(ACCESS_AUTH) {
        route("/admin") {
            get("/users") {
                call.requirePrincipal().requirePermission("users:read")
                val page = deps.listUsers.execute(call.pageRequest())
                call.respond(ApiResponse.ok(page.toPagedData { it.toResponse() }))
            }

            get("/users/{id}") {
                call.requirePrincipal().requirePermission("users:read")
                val id = call.userId()
                val user = deps.getUser.execute(id)
                call.respond(ApiResponse.ok(user.toResponse()))
            }

            post("/users/{id}/suspend") {
                call.requirePrincipal().requirePermission("users:write")
                val user = deps.setUserStatus.suspend(call.userId(), call.toRequestContext())
                call.respond(ApiResponse.ok(user.toResponse(), message = "User suspended"))
            }

            post("/users/{id}/reactivate") {
                call.requirePrincipal().requirePermission("users:write")
                val user = deps.setUserStatus.reactivate(call.userId(), call.toRequestContext())
                call.respond(ApiResponse.ok(user.toResponse(), message = "User reactivated"))
            }

            delete("/users/{id}") {
                call.requirePrincipal().requirePermission("users:delete")
                val user = deps.setUserStatus.softDelete(call.userId(), call.toRequestContext())
                call.respond(ApiResponse.ok(user.toResponse(), message = "User deleted"))
            }

            post("/users/{id}/roles") {
                call.requirePrincipal().requirePermission("roles:assign")
                val body = call.receive<AssignRoleRequest>()
                val user = deps.manageRoles.assign(call.userId(), body.role, call.toRequestContext())
                call.respond(ApiResponse.ok(user.toResponse(), message = "Role assigned"))
            }

            delete("/users/{id}/roles/{role}") {
                call.requirePrincipal().requirePermission("roles:assign")
                val role = call.parameters["role"] ?: throw NotFoundException("Role not specified")
                val user = deps.manageRoles.revoke(call.userId(), role, call.toRequestContext())
                call.respond(ApiResponse.ok(user.toResponse(), message = "Role revoked"))
            }

            get("/roles") {
                call.requirePrincipal().requirePermission("roles:read")
                val roles = deps.listRoles.execute()
                call.respond(ApiResponse.ok(roles.map { it.toResponse() }))
            }

            get("/audit-logs") {
                call.requirePrincipal().requirePermission("audit:read")
                val page = deps.listAuditLogs.execute(call.pageRequest())
                call.respond(ApiResponse.ok(page.toPagedData { it.toResponse() }))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userId(): Uuid {
    val raw = parameters["id"] ?: throw NotFoundException("User id not provided")
    return runCatching { Uuid.parse(raw) }.getOrElse { throw NotFoundException("Invalid user id") }
}

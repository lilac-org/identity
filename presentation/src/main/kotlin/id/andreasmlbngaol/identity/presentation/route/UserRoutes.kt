package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.usecase.account.ChangePasswordUseCase
import id.andreasmlbngaol.identity.domain.usecase.account.UpdateProfileUseCase
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.dto.ChangePasswordRequest
import id.andreasmlbngaol.identity.presentation.dto.UpdateProfileRequest
import id.andreasmlbngaol.identity.presentation.mapper.toResponse
import id.andreasmlbngaol.identity.presentation.plugin.ACCESS_AUTH
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import id.andreasmlbngaol.identity.presentation.security.requirePrincipal
import id.andreasmlbngaol.identity.presentation.security.toRequestContext
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDate

/** /api/v1/users — the authenticated user's own profile and credentials. */
fun Route.userRoutes(deps: ApiDependencies) {
    authenticate(ACCESS_AUTH) {
        route("/users") {
            get("/me") {
                val principal = call.requirePrincipal()
                val user = deps.getCurrentUser.execute(principal.userId)
                call.respond(ApiResponse.ok(user.toResponse()))
            }

            patch("/me") {
                val principal = call.requirePrincipal()
                val body = call.receive<UpdateProfileRequest>()
                val user = deps.updateProfile.execute(
                    UpdateProfileUseCase.Command(
                        userId = principal.userId,
                        fullName = body.fullName,
                        photoUrl = body.photoUrl,
                        phoneNumber = body.phoneNumber,
                        dateOfBirth = body.dateOfBirth?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        clearPhone = body.clearPhone,
                    ),
                    call.toRequestContext(),
                )
                call.respond(ApiResponse.ok(user.toResponse(), message = "Profile updated"))
            }

            post("/me/change-password") {
                val principal = call.requirePrincipal()
                val body = call.receive<ChangePasswordRequest>()
                deps.changePassword.execute(
                    ChangePasswordUseCase.Command(principal.userId, body.currentPassword, body.newPassword),
                    call.toRequestContext(),
                )
                call.respond(ApiResponse.ok<Unit>(null, message = "Password changed"))
            }
        }
    }
}

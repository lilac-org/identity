package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.usecase.account.ResetPasswordUseCase
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.domain.error.ClientException
import id.andreasmlbngaol.identity.domain.error.TokenException
import id.andreasmlbngaol.identity.domain.usecase.auth.LoginUseCase
import id.andreasmlbngaol.identity.domain.usecase.auth.LogoutUseCase
import id.andreasmlbngaol.identity.domain.usecase.auth.RefreshTokenUseCase
import id.andreasmlbngaol.identity.domain.usecase.auth.RegisterUseCase
import id.andreasmlbngaol.identity.domain.usecase.client.ClientCredentialsUseCase
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.dto.*
import id.andreasmlbngaol.identity.presentation.mapper.toResponse
import id.andreasmlbngaol.identity.presentation.plugin.ACCESS_AUTH
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import id.andreasmlbngaol.identity.presentation.security.requirePrincipal
import id.andreasmlbngaol.identity.presentation.security.toRequestContext
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * /api/v1/auth — registration, login, token lifecycle, password recovery, and
 * the (designed-but-gated) client-credentials grant.
 */
fun Route.authRoutes(deps: ApiDependencies) {
    route("/auth") {
        post("/register") {
            deps.rateLimiter.enforce(call, "register")
            val body = call.receive<RegisterRequest>()
            val user = deps.register.execute(
                RegisterUseCase.Command(
                    email = body.email,
                    username = body.username,
                    password = body.password,
                    fullName = body.fullName,
                    phoneNumber = body.phoneNumber,
                ),
                call.toRequestContext(),
            )
            call.respond(
                HttpStatusCode.Created,
                ApiResponse.ok(RegisterResponse(user = user.toResponse()), message = "Registration successful"),
            )
        }

        post("/login") {
            deps.rateLimiter.enforce(call, "login")
            val body = call.receive<LoginRequest>()
            val cookieClient = if (body.useCookie) {
                deps.resolvePublicClient.execute(
                    clientId = body.clientId.requiredCookieClientId(),
                    audiences = body.audience.toSet(),
                ).also { call.requireTrustedCookieRequest(it.allowedWebOrigins()) }
            } else null
            val audiences = body.audience.toSet().ifEmpty { cookieClient?.let { setOf(it.clientId) }.orEmpty() }
            val tokens = deps.login.execute(
                LoginUseCase.Command(body.identifier, body.password, audiences),
                call.toRequestContext().copy(clientId = cookieClient?.clientId),
            )
            if (body.useCookie) {
                call.setRefreshTokenCookie(
                    config = deps.httpConfig,
                    tokens = tokens,
                    maxAgeSeconds = deps.policy.refreshTokenTtl.inWholeSeconds,
                )
            }
            call.respond(
                ApiResponse.ok(
                    tokens.toResponse(includeRefreshToken = !body.useCookie),
                    message = "Login successful",
                ),
            )
        }

        post("/refresh") {
            val body = call.receive<RefreshRequest>()
            val cookieToken = call.refreshTokenCookie()
            val useCookie = body.refreshToken.isNullOrBlank() && cookieToken != null
            val cookieClient = if (useCookie) {
                deps.resolvePublicClient.execute(
                    clientId = body.clientId.requiredCookieClientId(),
                    audiences = body.audience.toSet(),
                ).also { call.requireTrustedCookieRequest(it.allowedWebOrigins()) }
            } else null
            val audiences = body.audience.toSet().ifEmpty { cookieClient?.let { setOf(it.clientId) }.orEmpty() }
            val refreshToken = body.refreshToken?.takeIf { it.isNotBlank() }
                ?: cookieToken
                ?: throw TokenException(ErrorCode.TOKEN_INVALID, "Refresh token is required")
            val tokens = deps.refresh.execute(
                RefreshTokenUseCase.Command(refreshToken, audiences),
                call.toRequestContext().copy(clientId = cookieClient?.clientId),
            )
            if (useCookie) {
                call.setRefreshTokenCookie(
                    config = deps.httpConfig,
                    tokens = tokens,
                    maxAgeSeconds = deps.policy.refreshTokenTtl.inWholeSeconds,
                )
            }
            call.respond(
                ApiResponse.ok(
                    tokens.toResponse(includeRefreshToken = !useCookie),
                    message = "Token refreshed",
                ),
            )
        }

        post("/forgot-password") {
            deps.rateLimiter.enforce(call, "forgot-password")
            val body = call.receive<ForgotPasswordRequest>()
            deps.forgotPassword.execute(body.email, call.toRequestContext())
            call.respond(
                ApiResponse.ok<Unit>(null, message = "If the email exists, a reset link has been sent"),
            )
        }

        post("/reset-password") {
            val body = call.receive<ResetPasswordRequest>()
            deps.resetPassword.execute(
                ResetPasswordUseCase.Command(body.token, body.newPassword),
                call.toRequestContext(),
            )
            call.respond(ApiResponse.ok<Unit>(null, message = "Password has been reset"))
        }

        post("/verify-email") {
            val token = call.request.queryParameters["token"]
                ?: call.receive<Map<String, String>>()["token"]
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.failure("Missing verification token", null),
                )
            val user = deps.verifyEmail.execute(token, call.toRequestContext())
            call.respond(ApiResponse.ok(user.toResponse(), message = "Email verified"))
        }

        post("/resend-verification") {
            deps.rateLimiter.enforce(call, "resend-verification")
            val body = call.receive<ResendVerificationRequest>()
            deps.resendVerification.execute(body.email)
            call.respond(
                ApiResponse.ok<Unit>(null, message = "If the email exists and is unverified, a link has been sent"),
            )
        }

        // OAuth2 client-credentials grant (service-to-service). Gated by config.
        post("/token") {
            val body = call.receive<ClientCredentialsRequest>()
            val tokens = deps.clientCredentials.execute(
                ClientCredentialsUseCase.Command(
                    clientId = body.clientId,
                    clientSecret = body.clientSecret,
                    scopes = body.scopes.toSet(),
                    audiences = body.audience.toSet(),
                ),
            )
            call.respond(ApiResponse.ok(tokens.toResponse(), message = "Token issued"))
        }

        authenticate(ACCESS_AUTH) {
            post("/logout") {
                val principal = call.requirePrincipal()
                val body = runCatching { call.receive<LogoutRequest>() }.getOrDefault(LogoutRequest())
                val cookieToken = call.refreshTokenCookie()
                val useCookie = body.refreshToken.isNullOrBlank() && cookieToken != null
                if (useCookie) {
                    val client = deps.resolvePublicClient.execute(
                        clientId = body.clientId.requiredCookieClientId(),
                        audiences = emptySet(),
                    )
                    call.requireTrustedCookieRequest(client.allowedWebOrigins())
                }
                deps.logout.execute(
                    LogoutUseCase.Command(
                        userId = principal.userId,
                        refreshToken = body.refreshToken?.takeIf { it.isNotBlank() } ?: cookieToken,
                        allDevices = body.allDevices,
                    ),
                    call.toRequestContext(),
                )
                if (useCookie) {
                    call.clearRefreshTokenCookie(deps.httpConfig)
                }
                call.respond(ApiResponse.ok<Unit>(null, message = "Logged out"))
            }
        }
    }
}

private fun String?.requiredCookieClientId(): String =
    this?.takeIf { it.isNotBlank() } ?: throw ClientException("Cookie authentication requires clientId")

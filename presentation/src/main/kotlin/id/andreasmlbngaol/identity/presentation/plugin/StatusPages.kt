package id.andreasmlbngaol.identity.presentation.plugin

import id.andreasmlbngaol.identity.domain.error.DomainException
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.presentation.response.ApiError
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

private val logger = KotlinLogging.logger {}

/**
 * Centralised error handling. Domain exceptions carry a stable [ErrorCode] that
 * is mapped to an HTTP status here, so use cases stay transport-agnostic and
 * clients receive a consistent error envelope.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<DomainException> { call, cause ->
            val status = cause.code.toHttpStatus()
            val details = cause.fieldErrors.takeIf { it.isNotEmpty() }
            if (status.value >= 500) logger.error(cause) { "Unhandled domain error: ${cause.code}" }
            call.respond(
                status,
                ApiResponse.failure(
                    message = cause.message,
                    error = ApiError(code = cause.code.name, details = details),
                ),
            )
        }
        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unexpected error" }
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse.failure(
                    message = "An unexpected error occurred",
                    error = ApiError(code = ErrorCode.INTERNAL.name),
                ),
            )
        }
    }
}

private fun ErrorCode.toHttpStatus(): HttpStatusCode = when (this) {
    ErrorCode.VALIDATION_FAILED -> HttpStatusCode.BadRequest
    ErrorCode.INVALID_CREDENTIALS -> HttpStatusCode.Unauthorized
    ErrorCode.TOKEN_INVALID, ErrorCode.TOKEN_EXPIRED, ErrorCode.TOKEN_REUSE_DETECTED -> HttpStatusCode.Unauthorized
    ErrorCode.EMAIL_NOT_VERIFIED, ErrorCode.ACCOUNT_NOT_ACTIVE, ErrorCode.PERMISSION_DENIED -> HttpStatusCode.Forbidden
    ErrorCode.USER_NOT_FOUND -> HttpStatusCode.NotFound
    ErrorCode.EMAIL_ALREADY_USED, ErrorCode.USERNAME_ALREADY_USED, ErrorCode.PHONE_ALREADY_USED,
    ErrorCode.EMAIL_ALREADY_VERIFIED, ErrorCode.CONFLICT -> HttpStatusCode.Conflict
    ErrorCode.CLIENT_INVALID -> HttpStatusCode.Unauthorized
    ErrorCode.OAUTH_FAILED -> HttpStatusCode.BadGateway
    ErrorCode.RATE_LIMITED -> HttpStatusCode.TooManyRequests
    ErrorCode.INTERNAL -> HttpStatusCode.InternalServerError
}

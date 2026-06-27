package id.andreasmlbngaol.identity.presentation.plugin

import id.andreasmlbngaol.identity.domain.error.DomainException
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.presentation.response.ApiError
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

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
        // Malformed or incomplete JSON bodies (e.g. missing required fields) are
        // client errors, not server errors -> 400 with a stable code. Ktor wraps
        // content-negotiation failures in BadRequestException, but the underlying
        // SerializationException can also surface directly, so handle both.
        exception<BadRequestException> { call, cause ->
            call.respondBadBody(cause)
        }
        exception<SerializationException> { call, cause ->
            call.respondBadBody(cause)
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

/** Maps a malformed-request-body failure to a 400 with a VALIDATION_FAILED code. */
private suspend fun ApplicationCall.respondBadBody(cause: Throwable) {
    logger.debug(cause) { "Rejected malformed request body" }
    // Ktor wraps the kotlinx.serialization error a few layers deep
    // (BadRequestException -> JsonConvertException -> SerializationException).
    // Walk the cause chain to surface the most specific serializer message,
    // e.g. "Field 'email' is required ... but it was missing". The trailing
    // " at path: $" is a kotlinx.serialization JSON pointer that is noise for
    // flat bodies, so trim it off for the client-facing message.
    val reason = (cause.findSerializationMessage() ?: cause.message)
        ?.substringBefore(" at path:")
        ?.trim()
    respond(
        HttpStatusCode.BadRequest,
        ApiResponse.failure(
            message = reason?.takeIf { it.isNotBlank() } ?: "Invalid or malformed request body",
            error = ApiError(code = ErrorCode.VALIDATION_FAILED.name),
        ),
    )
}

/** Walks the exception cause chain and returns the first SerializationException message. */
private fun Throwable.findSerializationMessage(): String? {
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth < 10) {
        if (current is SerializationException) return current.message
        current = current.cause
        depth++
    }
    return null
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

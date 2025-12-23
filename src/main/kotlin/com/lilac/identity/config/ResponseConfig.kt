package com.lilac.identity.config

import com.lilac.identity.domain.model.CreateUserException
import com.lilac.identity.domain.model.CreateUserProfileException
import com.lilac.identity.domain.model.EmailAlreadyUsedException
import com.lilac.identity.domain.model.EmailVerificationNotSentException
import com.lilac.identity.domain.model.InternalServerException
import com.lilac.identity.domain.model.UserNotFoundException
import com.lilac.identity.domain.model.UsernameAlreadyUsedException
import com.lilac.identity.presentation.dto.ErrorResponse
import com.lilac.identity.util.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureResponse() {
    install(StatusPages) {
        exception<EmailAlreadyUsedException> { call, cause ->
            call.respondError(
                HttpStatusCode.Conflict,
                cause.message
            )
        }

        exception<UsernameAlreadyUsedException> { call, cause ->
            call.respondError(
                HttpStatusCode.Conflict,
                cause.message
            )
        }

        exception<EmailVerificationNotSentException> { call, cause ->
            call.respondError(
                HttpStatusCode.InternalServerError,
                cause.message
            )
        }

        exception<UserNotFoundException> { call, cause ->
            call.respondError(
                HttpStatusCode.NotFound,
                cause.message
            )
        }

        exception<CreateUserException> { call, cause ->
            call.respondError(
                HttpStatusCode.InternalServerError,
                cause.message
            )
        }

        exception<CreateUserProfileException> { call, cause ->
            call.respondError(
                HttpStatusCode.InternalServerError,
                cause.message
            )
        }

        exception<InternalServerException> { call, cause ->
            call.respondError(
                HttpStatusCode.InternalServerError,
                cause.message
            )
        }

        exception<ContentTransformationException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid request payload")
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Bad Request")
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(cause.message ?: "Internal Server Error")
            )
        }
    }
}
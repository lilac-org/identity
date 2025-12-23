package com.lilac.identity.util

import com.lilac.identity.presentation.dto.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    message: String?
) = this.respond(status, ErrorResponse(message ?: "Something went wrong"))
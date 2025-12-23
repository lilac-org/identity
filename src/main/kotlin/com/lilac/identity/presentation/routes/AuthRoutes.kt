package com.lilac.identity.presentation.routes

import com.lilac.identity.domain.usecase.AuthUseCae
import com.lilac.identity.presentation.mapper.toDto
import com.lilac.identity.presentation.request.RegisterRequest
import com.lilac.identity.presentation.response.TokenPairResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val authUseCase by inject<AuthUseCae>()

    route("/auth") {
        post("/register") {
            val payload = call.receive<RegisterRequest>()

            val tokenPair = authUseCase.registerUser(
                email = payload.email,
                username = payload.username,
                password = payload.password,
                firstName = payload.firstName,
                lastName = payload.lastName
            )

            call.respond(
                HttpStatusCode.Created,
                TokenPairResponse(
                    data = tokenPair.toDto()
                )
            )
        }
    }
}
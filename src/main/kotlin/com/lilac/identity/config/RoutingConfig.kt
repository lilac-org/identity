package com.lilac.identity.config

import com.lilac.identity.presentation.response.HelloResponse
import com.lilac.identity.presentation.routes.authRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") { call.respondRedirect("/api") }
        openAPI(path="openapi", swaggerFile = "openapi/documentation.json")
        swaggerUI(path="swagger", swaggerFile = "openapi/documentation.json")

        route("/api") {
            get {
                call.respond(
                    HttpStatusCode.OK,
                    HelloResponse()
                )
            }

            authRoutes()
        }
    }
}
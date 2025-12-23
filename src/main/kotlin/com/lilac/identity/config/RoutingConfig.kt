package com.lilac.identity.config

import io.ktor.server.application.Application
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") { call.respondRedirect("/api") }

        route("/api") {

        }
    }
}
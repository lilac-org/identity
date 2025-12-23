package com.lilac.identity.config

import dev.hayden.KHealth
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.configureHealth() {
    install(KHealth) {
        successfulCheckStatusCode = HttpStatusCode.Accepted
        unsuccessfulCheckStatusCode = HttpStatusCode.ExpectationFailed
    }
}
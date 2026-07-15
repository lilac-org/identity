package id.andreasmlbngaol.identity.presentation.plugin

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.request.httpMethod
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.server.response.appendIfAbsent
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.util.UUID

/** JSON (kotlinx.serialization) content negotiation shared by all routes. */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
                isLenient = true
            },
        )
    }
}

/**
 * Request correlation + structured access logs. Every request gets a stable
 * call id (propagated from X-Request-Id when present) that is attached to the
 * MDC so it appears in every log line for that request — essential for tracing
 * across services.
 */
fun Application.configureMonitoring() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("callId")
        filter { call -> call.request.local.uri.startsWith("/api") }
    }
}

/**
 * Transport concerns: permissive CORS for local development (lock this down per
 * environment), gzip compression, and sensible default headers.
 */
fun Application.configureHttp(allowedHosts: List<String>, behindProxy: Boolean = false) {
    // Behind a trusted reverse proxy (TLS terminator) honor X-Forwarded-* so that
    // call.request.origin reflects the public scheme/host/port. Enable ONLY when
    // actually behind a proxy, otherwise clients could spoof these headers.
    if (behindProxy) {
        install(XForwardedHeaders)
    }
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
    }
    install(Compression) { gzip { priority = 1.0 } }
    install(CORS) {
        allowNonSimpleContentTypes = true
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.XRequestId)
        allowHeader("X-Requested-With")
        allowCredentials = true
        if (allowedHosts.isEmpty() || allowedHosts.contains("*")) {
            anyHost() // development default
        } else {
            allowedHosts.forEach { host ->
                val parts = host.split("://")
                if (parts.size == 2) allowHost(parts[1], schemes = listOf(parts[0])) else allowHost(host)
            }
        }
    }
    // Ktor 3.5.1 still omits Access-Control-Allow-Credentials on automatic
    // preflight responses even when allowCredentials = true. Credentialed
    // browser fetches require it, so patch only those OPTIONS preflight
    // responses at send time after the CORS plugin has built the response.
    sendPipeline.intercept(ApplicationSendPipeline.Before) {
        if (
            call.request.httpMethod == HttpMethod.Options &&
            call.request.headers["Origin"] != null &&
            call.request.headers["Access-Control-Request-Method"] != null
        ) {
            call.response.headers.appendIfAbsent("Access-Control-Allow-Credentials", "true")
        }
        proceed()
    }
}

package id.andreasmlbngaol.identity.presentation

import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.presentation.admin.adminDashboardRoutes
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.plugin.configureAdminSessions
import id.andreasmlbngaol.identity.presentation.plugin.configureHttp
import id.andreasmlbngaol.identity.presentation.plugin.configureMonitoring
import id.andreasmlbngaol.identity.presentation.plugin.configureSecurity
import id.andreasmlbngaol.identity.presentation.plugin.configureSerialization
import id.andreasmlbngaol.identity.presentation.plugin.configureStatusPages
import id.andreasmlbngaol.identity.presentation.route.adminRoutes
import id.andreasmlbngaol.identity.presentation.route.authRoutes
import id.andreasmlbngaol.identity.presentation.route.oauthRoutes
import id.andreasmlbngaol.identity.presentation.route.systemRoutes
import id.andreasmlbngaol.identity.presentation.route.userRoutes
import id.andreasmlbngaol.identity.presentation.route.verificationPages
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey

/**
 * Single entry point the app layer calls to wire the entire HTTP surface:
 * plugins, security, and routing. Keeping this here means the app module only
 * has to construct [ApiDependencies] and hand over the verification key.
 */
fun Application.installPresentation(
    deps: ApiDependencies,
    publicKey: RSAPublicKey,
    userRepository: UserRepository,
) {
    configureSerialization()
    configureMonitoring()
    configureHttp(deps.httpConfig.allowedHosts, deps.httpConfig.behindProxy)
    configureStatusPages()
    configureSecurity(publicKey, deps.policy, userRepository)

    if (deps.httpConfig.adminDashboardEnabled) {
        // Derive a stable cookie-signing key from the RSA public key so admin
        // sessions survive restarts without introducing a new secret/env var.
        val signKey = MessageDigest.getInstance("SHA-256").digest(publicKey.encoded)
        configureAdminSessions(signKey, deps.httpConfig.cookieSecure)
    }

    routing {
        // Operational + discovery endpoints (unauthenticated).
        systemRoutes(deps)

        // Versioned JSON API.
        route("/api/v1") {
            authRoutes(deps)
            userRoutes(deps)
            oauthRoutes(deps)
            adminRoutes(deps)
        }

        // Server-rendered email-verification & password-reset pages (HTML).
        // Paths mirror FrontendLinks; point FRONTEND_BASE_URL at this service.
        verificationPages(deps)

        // Server-rendered admin dashboard (HTML + HTMX), protected by an
        // ADMIN-only cookie login.
        if (deps.httpConfig.adminDashboardEnabled) {
            adminDashboardRoutes(deps, userRepository)
        }

        // Swagger UI served from static resources when enabled.
        if (deps.httpConfig.swaggerEnabled) {
            get("/") { call.respondRedirect("/swagger") }
            staticResources("/swagger", "openapi")
        }
    }
}

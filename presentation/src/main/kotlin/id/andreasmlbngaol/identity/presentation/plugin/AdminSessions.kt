package id.andreasmlbngaol.identity.presentation.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable

/**
 * The signed cookie payload that backs the admin dashboard login. Only the
 * minimum is stored; authorization (ADMIN role) is re-checked at login time and
 * the cookie is HMAC-signed so it cannot be forged client-side.
 */
@Serializable
data class AdminSession(
    val userId: String,
    val email: String,
)

const val ADMIN_SESSION_COOKIE = "admin_session"

/**
 * Installs cookie-based sessions for the server-rendered admin dashboard.
 *
 * The [signKey] is derived from the service's RSA public key by the caller, so
 * the signature is stable across restarts without requiring an extra secret or
 * environment variable. The cookie is HttpOnly and scoped to the dashboard
 * path; enable [io.ktor.http.Cookie.secure] behind HTTPS in production.
 */
fun Application.configureAdminSessions(signKey: ByteArray, secure: Boolean = false) {
    install(Sessions) {
        cookie<AdminSession>(ADMIN_SESSION_COOKIE) {
            cookie.path = "/admin/dashboard"
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = 8 * 60 * 60
            // Driven by COOKIE_SECURE; set true in production so the cookie is HTTPS-only.
            cookie.secure = secure
            transform(SessionTransportTransformerMessageAuthentication(signKey))
        }
    }
}

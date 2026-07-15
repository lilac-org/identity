package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.error.PermissionDeniedException
import id.andreasmlbngaol.identity.domain.model.AuthTokens
import id.andreasmlbngaol.identity.domain.model.Client
import id.andreasmlbngaol.identity.presentation.config.HttpRuntimeConfig
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import java.net.URI

internal const val COOKIE_REQUEST_HEADER = "X-Requested-With"
internal const val COOKIE_REQUEST_VALUE = "AFinance"
private const val REFRESH_COOKIE_NAME = "identity_refresh"
private const val REFRESH_COOKIE_PATH = "/api/v1/auth"

internal fun ApplicationCall.requireTrustedCookieRequest(allowedOrigins: Set<String>) {
    if (!isTrustedCookieRequest(
            origin = request.headers[HttpHeaders.Origin],
            requestedWith = request.headers[COOKIE_REQUEST_HEADER],
            allowedOrigins = allowedOrigins,
        )
    ) {
        throw PermissionDeniedException("Cookie authentication request is not trusted")
    }
}

internal fun isTrustedCookieRequest(
    origin: String?,
    requestedWith: String?,
    allowedOrigins: Set<String>,
): Boolean = normalizeOrigin(origin) in allowedOrigins && requestedWith == COOKIE_REQUEST_VALUE

internal fun Client.allowedWebOrigins(): Set<String> = redirectUris.mapNotNull(::normalizeOrigin).toSet()

private fun normalizeOrigin(value: String?): String? {
    val uri = value?.let { runCatching { URI(it) }.getOrNull() } ?: return null
    val scheme = uri.scheme?.lowercase()?.takeIf { it == "http" || it == "https" } ?: return null
    val host = uri.host?.lowercase() ?: return null
    val port = uri.port.takeIf { it > 0 && !((scheme == "https" && it == 443) || (scheme == "http" && it == 80)) }
    return "$scheme://$host${port?.let { ":$it" }.orEmpty()}"
}

internal fun ApplicationCall.refreshTokenCookie(): String? = request.cookies[REFRESH_COOKIE_NAME]

internal fun ApplicationCall.setRefreshTokenCookie(
    config: HttpRuntimeConfig,
    tokens: AuthTokens,
    maxAgeSeconds: Long,
) {
    response.cookies.append(
        name = REFRESH_COOKIE_NAME,
        value = tokens.refreshToken,
        maxAge = maxAgeSeconds,
        path = REFRESH_COOKIE_PATH,
        secure = config.cookieSecure,
        httpOnly = true,
        extensions = mapOf("SameSite" to if (config.cookieSecure) "None" else "Lax"),
    )
}

internal fun ApplicationCall.clearRefreshTokenCookie(config: HttpRuntimeConfig) {
    response.cookies.append(
        name = REFRESH_COOKIE_NAME,
        value = "",
        maxAge = 0,
        path = REFRESH_COOKIE_PATH,
        secure = config.cookieSecure,
        httpOnly = true,
        extensions = mapOf("SameSite" to if (config.cookieSecure) "None" else "Lax"),
    )
}

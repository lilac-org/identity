package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.enums.AuthProvider
import id.andreasmlbngaol.identity.domain.error.OAuthException
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.mapper.toResponse
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import id.andreasmlbngaol.identity.presentation.security.toRequestContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.Base64
import java.util.UUID

/**
 * /api/v1/oauth — Authorization-Code flow for Google and GitHub.
 *
 * `GET /{provider}` redirects the user to the provider with a signed-ish state
 * value; `GET /{provider}/callback` exchanges the code and returns tokens. The
 * `state` parameter is round-tripped to mitigate CSRF.
 */
fun Route.oauthRoutes(deps: ApiDependencies) {
    route("/oauth") {
        get("/{provider}") {
            val provider = call.parseProvider() ?: return@get
            val redirectUri = call.redirectUri(provider)
            val state = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().toByteArray())
            val url = deps.oauthLogin.authorizationUrl(provider, state, redirectUri)
            call.respondRedirect(url)
        }

        get("/{provider}/callback") {
            val provider = call.parseProvider() ?: return@get
            val code = call.request.queryParameters["code"]
                ?: throw OAuthException("Missing authorization code")
            val redirectUri = call.redirectUri(provider)
            val tokens = deps.oauthLogin.callback(
                provider = provider,
                code = code,
                redirectUri = redirectUri,
                audiences = emptySet(),
                ctx = call.toRequestContext(),
            )
            call.respond(ApiResponse.ok(tokens.toResponse(), message = "OAuth login successful"))
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.parseProvider(): AuthProvider? {
    val raw = parameters["provider"]?.uppercase()
    return runCatching { AuthProvider.valueOf(raw ?: "") }.getOrNull()
        ?.takeIf { it != AuthProvider.LOCAL }
        ?: run {
            respond(HttpStatusCode.BadRequest, ApiResponse.failure("Unsupported OAuth provider", null))
            null
        }
}

private fun io.ktor.server.application.ApplicationCall.redirectUri(provider: AuthProvider): String =
    request.queryParameters["redirect_uri"]
        ?: "${request.local.scheme}://${request.host()}:${request.port()}/api/v1/oauth/${provider.name.lowercase()}/callback"

private fun io.ktor.server.request.ApplicationRequest.host(): String = local.serverHost
private fun io.ktor.server.request.ApplicationRequest.port(): Int = local.serverPort

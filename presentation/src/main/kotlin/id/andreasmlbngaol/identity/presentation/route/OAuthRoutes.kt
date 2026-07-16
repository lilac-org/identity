package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.enums.AuthProvider
import id.andreasmlbngaol.identity.domain.error.OAuthException
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.dto.AuthorizationCodeExchangeRequest
import id.andreasmlbngaol.identity.presentation.mapper.toResponse
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import id.andreasmlbngaol.identity.presentation.security.toRequestContext
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.oauthRoutes(deps: ApiDependencies) {
    route("/oauth") {
        get("/{provider}") {
            val provider = call.parseProvider() ?: return@get
            val clientId = call.request.queryParameters["client_id"] ?: throw OAuthException("Missing client_id")
            val redirectUri = call.request.queryParameters["redirect_uri"] ?: throw OAuthException("Missing redirect_uri")
            val state = call.request.queryParameters["state"] ?: throw OAuthException("Missing state")
            val codeChallenge = call.request.queryParameters["code_challenge"] ?: throw OAuthException("Missing code_challenge")
            if (call.request.queryParameters["code_challenge_method"] != "S256") throw OAuthException("PKCE S256 is required")
            call.respondRedirect(deps.oauthLogin.beginAuthorization(
                provider, clientId, redirectUri, state, codeChallenge, call.identityCallbackUri(provider),
            ))
        }
        get("/{provider}/callback") {
            val provider = call.parseProvider() ?: return@get
            val code = call.request.queryParameters["code"] ?: throw OAuthException("Missing authorization code")
            val state = call.request.queryParameters["state"] ?: throw OAuthException("Missing OAuth state")
            val result = deps.oauthLogin.completeAuthorization(
                provider, code, state, call.identityCallbackUri(provider), call.toRequestContext(),
            )
            val destination = URLBuilder(result.redirectUri).apply {
                parameters.append("code", result.code)
                parameters.append("state", result.state)
            }.buildString()
            call.respondRedirect(destination)
        }
        post("/token") {
            val body = call.receive<AuthorizationCodeExchangeRequest>()
            val tokens = deps.oauthLogin.exchangeAuthorizationCode(
                body.clientId, body.code, body.redirectUri, body.codeVerifier, call.toRequestContext(),
            )
            call.respond(ApiResponse.ok(tokens.toResponse(), message = "OAuth token issued"))
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.parseProvider(): AuthProvider? {
    val provider = runCatching { AuthProvider.valueOf(parameters["provider"]?.uppercase() ?: "") }.getOrNull()
        ?.takeIf { it != AuthProvider.LOCAL }
    if (provider == null) respond(HttpStatusCode.BadRequest, ApiResponse.failure("Unsupported OAuth provider", null))
    return provider
}

private fun io.ktor.server.application.ApplicationCall.identityCallbackUri(provider: AuthProvider): String {
    val origin = request.origin
    val authority = if ((origin.scheme == "https" && origin.serverPort == 443) || (origin.scheme == "http" && origin.serverPort == 80)) {
        origin.serverHost
    } else "${origin.serverHost}:${origin.serverPort}"
    return "${origin.scheme}://$authority/api/v1/oauth/${provider.name.lowercase()}/callback"
}

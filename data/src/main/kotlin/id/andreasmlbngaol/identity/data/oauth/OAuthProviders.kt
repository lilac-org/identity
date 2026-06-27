package id.andreasmlbngaol.identity.data.oauth

import id.andreasmlbngaol.identity.data.config.OAuthProviderConfig
import id.andreasmlbngaol.identity.domain.enums.AuthProvider
import id.andreasmlbngaol.identity.domain.error.OAuthException
import id.andreasmlbngaol.identity.domain.service.OAuthProvider
import id.andreasmlbngaol.identity.domain.service.OAuthUserProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray

private val json = Json { ignoreUnknownKeys = true }

/** Google OpenID Connect: exchange code -> access token -> userinfo. */
class GoogleOAuthProvider(
    private val config: OAuthProviderConfig,
    private val httpClient: HttpClient,
) : OAuthProvider {
    override val provider = AuthProvider.GOOGLE

    override fun authorizationUrl(state: String, redirectUri: String): String =
        URLBuilder("https://accounts.google.com/o/oauth2/v2/auth").apply {
            parameters.append("client_id", config.clientId)
            parameters.append("redirect_uri", redirectUri)
            parameters.append("response_type", "code")
            parameters.append("scope", "openid email profile")
            parameters.append("state", state)
            parameters.append("access_type", "offline")
        }.buildString()

    override suspend fun exchangeCode(code: String, redirectUri: String): OAuthUserProfile {
        val tokenResponse: JsonObject = try {
            httpClient.submitForm(
                url = "https://oauth2.googleapis.com/token",
                formParameters = Parameters.build {
                    append("code", code)
                    append("client_id", config.clientId)
                    append("client_secret", config.clientSecret)
                    append("redirect_uri", redirectUri)
                    append("grant_type", "authorization_code")
                },
            ).body()
        } catch (e: Exception) {
            throw OAuthException("Failed to exchange Google authorization code", e)
        }
        val accessToken = tokenResponse["access_token"]?.jsonPrimitive?.contentOrNull
            ?: throw OAuthException("Google did not return an access token")

        val userInfo: JsonObject = httpClient.get("https://openidconnect.googleapis.com/v1/userinfo") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()

        return OAuthUserProfile(
            provider = AuthProvider.GOOGLE,
            providerUserId = userInfo["sub"]?.jsonPrimitive?.contentOrNull
                ?: throw OAuthException("Google profile missing subject"),
            email = userInfo["email"]?.jsonPrimitive?.contentOrNull,
            emailVerified = userInfo["email_verified"]?.jsonPrimitive?.booleanOrNull ?: false,
            fullName = userInfo["name"]?.jsonPrimitive?.contentOrNull,
            photoUrl = userInfo["picture"]?.jsonPrimitive?.contentOrNull,
        )
    }
}

/** GitHub OAuth: exchange code -> token -> /user (+ /user/emails). */
class GitHubOAuthProvider(
    private val config: OAuthProviderConfig,
    private val httpClient: HttpClient,
) : OAuthProvider {
    override val provider = AuthProvider.GITHUB

    override fun authorizationUrl(state: String, redirectUri: String): String =
        URLBuilder("https://github.com/login/oauth/authorize").apply {
            parameters.append("client_id", config.clientId)
            parameters.append("redirect_uri", redirectUri)
            parameters.append("scope", "read:user user:email")
            parameters.append("state", state)
        }.buildString()

    override suspend fun exchangeCode(code: String, redirectUri: String): OAuthUserProfile {
        val tokenResponse: JsonObject = try {
            httpClient.submitForm(
                url = "https://github.com/login/oauth/access_token",
                formParameters = Parameters.build {
                    append("code", code)
                    append("client_id", config.clientId)
                    append("client_secret", config.clientSecret)
                    append("redirect_uri", redirectUri)
                },
            ) { accept(ContentType.Application.Json) }.body()
        } catch (e: Exception) {
            throw OAuthException("Failed to exchange GitHub authorization code", e)
        }
        val accessToken = tokenResponse["access_token"]?.jsonPrimitive?.contentOrNull
            ?: throw OAuthException("GitHub did not return an access token")

        val user: JsonObject = httpClient.get("https://api.github.com/user") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            accept(ContentType.Application.Json)
        }.body()

        // Primary, verified email may require the dedicated endpoint.
        var email = user["email"]?.jsonPrimitive?.contentOrNull
        var emailVerified = false
        if (email == null) {
            val emails = httpClient.get("https://api.github.com/user/emails") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                accept(ContentType.Application.Json)
            }.body<List<JsonObject>>()
            val primary = emails.firstOrNull { it["primary"]?.jsonPrimitive?.booleanOrNull == true }
            email = primary?.get("email")?.jsonPrimitive?.contentOrNull
            emailVerified = primary?.get("verified")?.jsonPrimitive?.booleanOrNull ?: false
        }

        return OAuthUserProfile(
            provider = AuthProvider.GITHUB,
            providerUserId = (user["id"]?.jsonPrimitive?.contentOrNull)
                ?: throw OAuthException("GitHub profile missing id"),
            email = email,
            emailVerified = emailVerified,
            fullName = user["name"]?.jsonPrimitive?.contentOrNull,
            photoUrl = user["avatar_url"]?.jsonPrimitive?.contentOrNull,
        )
    }
}

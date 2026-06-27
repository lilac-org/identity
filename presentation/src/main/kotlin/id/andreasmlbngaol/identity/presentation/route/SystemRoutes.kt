package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import io.ktor.http.ContentType
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray

/**
 * Unauthenticated operational endpoints:
 *  - GET /health                      liveness probe
 *  - GET /.well-known/jwks.json       public keys for token verification
 *  - GET /.well-known/openid-configuration  minimal discovery document
 */
fun Route.systemRoutes(deps: ApiDependencies) {
    get("/health") {
        call.respond(ApiResponse.ok(mapOf("status" to "UP"), message = "Service healthy"))
    }

    get("/.well-known/jwks.json") {
        // The issuer produces a JSON-serializable map; emit it verbatim.
        val jwks = deps.jwksProvider()
        call.respondText(toJson(jwks).toString(), ContentType.Application.Json)
    }

    get("/.well-known/openid-configuration") {
        val issuer = deps.policy.issuer
        call.respond(
            mapOf(
                "issuer" to issuer,
                "jwks_uri" to "/.well-known/jwks.json",
                "token_endpoint" to "/api/v1/auth/token",
                "id_token_signing_alg_values_supported" to listOf("RS256"),
            ),
        )
    }
}

/** Converts the nimbus JWKS map into a kotlinx JsonElement for serialization. */
private fun toJson(value: Any?): JsonElement = when (value) {
    null -> JsonPrimitive(null as String?)
    is String -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJson(v) })
    is Collection<*> -> JsonArray(value.map { toJson(it) })
    is Array<*> -> JsonArray(value.map { toJson(it) })
    else -> JsonPrimitive(value.toString())
}

package id.andreasmlbngaol.identity.presentation.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import id.andreasmlbngaol.identity.domain.enums.TokenType
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.presentation.response.ApiError
import id.andreasmlbngaol.identity.presentation.response.ApiResponse
import id.andreasmlbngaol.identity.presentation.security.AuthenticatedPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import java.security.interfaces.RSAPublicKey
import kotlin.uuid.Uuid

const val ACCESS_AUTH = "access-jwt"

/**
 * Configures stateless JWT bearer authentication.
 *
 * Verification uses only the RSA public key + issuer, so this works
 * identically in any downstream service that imports the public key. We then
 * compare the token's `tokenVersion` against the stored value to honour global
 * invalidation (logout-everywhere, password reset, role changes).
 */
fun Application.configureSecurity(
    publicKey: RSAPublicKey,
    policy: AuthPolicy,
    userRepository: UserRepository,
) {
    install(Authentication) {
        jwt(ACCESS_AUTH) {
            realm = policy.issuer
            verifier(
                JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer(policy.issuer)
                    .build(),
            )
            validate { credential ->
                val payload = credential.payload
                val type = payload.getClaim("type").asString()
                val subject = payload.subject ?: return@validate null

                when (type) {
                    TokenType.ACCESS.name -> {
                        val userId = runCatching { Uuid.parse(subject) }.getOrNull() ?: return@validate null
                        val tokenVersion = payload.getClaim("tokenVersion").asInt() ?: 0
                        val user = userRepository.findById(userId) ?: return@validate null
                        if (!user.isActive || user.tokenVersion != tokenVersion) return@validate null
                        AuthenticatedPrincipal(
                            userId = userId,
                            tokenType = TokenType.ACCESS,
                            roles = user.roleNames,
                            permissions = user.permissionNames,
                            audiences = payload.audience?.toSet() ?: emptySet(),
                            tokenId = payload.id ?: "",
                        )
                    }
                    TokenType.SERVICE.name -> AuthenticatedPrincipal(
                        userId = Uuid.NIL,
                        tokenType = TokenType.SERVICE,
                        roles = emptySet(),
                        permissions = (payload.getClaim("permissions").asList(String::class.java) ?: emptyList()).toSet(),
                        audiences = payload.audience?.toSet() ?: emptySet(),
                        tokenId = payload.id ?: "",
                    )
                    else -> null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.failure(
                        message = "Authentication required or token invalid",
                        error = ApiError(code = "TOKEN_INVALID"),
                    ),
                )
            }
        }
    }
}

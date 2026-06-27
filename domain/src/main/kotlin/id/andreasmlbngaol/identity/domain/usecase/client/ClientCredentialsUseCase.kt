package id.andreasmlbngaol.identity.domain.usecase.client

import id.andreasmlbngaol.identity.domain.error.ClientException
import id.andreasmlbngaol.identity.domain.model.AuthTokens
import id.andreasmlbngaol.identity.domain.repository.ClientRepository
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.PasswordHasher
import id.andreasmlbngaol.identity.domain.service.TokenIssuer

/**
 * OAuth2 client-credentials grant for service-to-service auth. Designed and
 * wired now; intended to be enabled in a later phase as agreed.
 */
class ClientCredentialsUseCase(
    private val clients: ClientRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenIssuer: TokenIssuer,
    private val clock: Clock,
) {
    data class Command(
        val clientId: String,
        val clientSecret: String,
        val scopes: Set<String> = emptySet(),
        val audiences: Set<String> = emptySet(),
    )

    suspend fun execute(command: Command): AuthTokens {
        val client = clients.findByClientId(command.clientId)?.takeIf { it.enabled }
            ?: throw ClientException("Unknown or disabled client")
        val secretHash = client.clientSecretHash ?: throw ClientException("Client is not confidential")
        if (!passwordHasher.verify(command.clientSecret, secretHash)) throw ClientException("Invalid client secret")

        val grantedScopes = if (command.scopes.isEmpty()) client.allowedScopes
        else command.scopes.intersect(client.allowedScopes)
        val grantedAudiences = if (command.audiences.isEmpty()) client.allowedAudiences
        else command.audiences.intersect(client.allowedAudiences)

        val token = tokenIssuer.issueServiceToken(client.clientId, grantedScopes, grantedAudiences)
        return AuthTokens(
            accessToken = token.token,
            accessTokenExpiresAt = token.expiresAt,
            refreshToken = "",
            refreshTokenExpiresAt = token.expiresAt,
        )
    }
}

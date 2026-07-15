package id.andreasmlbngaol.identity.domain.usecase.client

import id.andreasmlbngaol.identity.domain.error.ClientException
import id.andreasmlbngaol.identity.domain.model.Client
import id.andreasmlbngaol.identity.domain.repository.ClientRepository

class ResolvePublicClientUseCase(
    private val clients: ClientRepository,
) {
    suspend fun execute(clientId: String, audiences: Set<String>): Client {
        val client = clients.findByClientId(clientId)?.takeIf { it.enabled }
            ?: throw ClientException("Unknown or disabled client")
        if (client.isConfidential) throw ClientException("Cookie authentication requires a public client")
        val requestedAudiences = audiences.ifEmpty { setOf(clientId) }
        if (!client.allowedAudiences.containsAll(requestedAudiences)) {
            throw ClientException("Client is not allowed to request the audience")
        }
        return client
    }
}

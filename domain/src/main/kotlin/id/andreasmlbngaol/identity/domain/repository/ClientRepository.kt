package id.andreasmlbngaol.identity.domain.repository

import id.andreasmlbngaol.identity.domain.model.Client

interface ClientRepository {
    suspend fun findByClientId(clientId: String): Client?
    suspend fun findAll(): List<Client>
    suspend fun create(client: Client): Client
    suspend fun update(client: Client): Client
}

package id.andreasmlbngaol.identity.data.repository

import id.andreasmlbngaol.identity.data.db.AuditLogsTable
import id.andreasmlbngaol.identity.data.db.ClientsTable
import id.andreasmlbngaol.identity.data.db.OAuthAccountsTable
import id.andreasmlbngaol.identity.data.db.dbQuery
import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.enums.AuthProvider
import id.andreasmlbngaol.identity.domain.model.AuditLog
import id.andreasmlbngaol.identity.domain.model.Client
import id.andreasmlbngaol.identity.domain.model.OAuthAccount
import id.andreasmlbngaol.identity.domain.model.PageRequest
import id.andreasmlbngaol.identity.domain.model.PageResult
import id.andreasmlbngaol.identity.domain.repository.AuditLogRepository
import id.andreasmlbngaol.identity.domain.repository.ClientRepository
import id.andreasmlbngaol.identity.domain.repository.OAuthAccountRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

private val json = Json { ignoreUnknownKeys = true }
private fun encodeCsv(values: Set<String>): String = values.joinToString(",")
private fun decodeCsv(value: String): Set<String> =
    value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

class OAuthAccountRepositoryImpl : OAuthAccountRepository {
    override suspend fun findByProvider(provider: AuthProvider, providerUserId: String): OAuthAccount? = dbQuery {
        OAuthAccountsTable.selectAll().where {
            (OAuthAccountsTable.provider eq provider.name) and (OAuthAccountsTable.providerUserId eq providerUserId)
        }.firstOrNull()?.toOAuthAccount()
    }

    override suspend fun findByUserId(userId: Uuid): List<OAuthAccount> = dbQuery {
        OAuthAccountsTable.selectAll().where { OAuthAccountsTable.userId eq userId }
            .map { it.toOAuthAccount() }
    }

    override suspend fun link(account: OAuthAccount): OAuthAccount = dbQuery {
        OAuthAccountsTable.insert { row ->
            row[id] = account.id
            row[userId] = account.userId
            row[provider] = account.provider.name
            row[providerUserId] = account.providerUserId
            row[email] = account.email
            row[createdAt] = account.createdAt
        }
        account
    }

    private fun ResultRow.toOAuthAccount() = OAuthAccount(
        id = this[OAuthAccountsTable.id],
        userId = this[OAuthAccountsTable.userId],
        provider = AuthProvider.valueOf(this[OAuthAccountsTable.provider]),
        providerUserId = this[OAuthAccountsTable.providerUserId],
        email = this[OAuthAccountsTable.email],
        createdAt = this[OAuthAccountsTable.createdAt],
    )
}

class ClientRepositoryImpl : ClientRepository {
    override suspend fun findByClientId(clientId: String): Client? = dbQuery {
        ClientsTable.selectAll().where { ClientsTable.clientId eq clientId }.firstOrNull()?.toClient()
    }

    override suspend fun findAll(): List<Client> = dbQuery {
        ClientsTable.selectAll().orderBy(ClientsTable.name).map { it.toClient() }
    }

    override suspend fun create(client: Client): Client = dbQuery {
        ClientsTable.insert { row ->
            row[id] = client.id
            row[clientId] = client.clientId
            row[clientSecretHash] = client.clientSecretHash
            row[name] = client.name
            row[allowedAudiences] = encodeCsv(client.allowedAudiences)
            row[allowedScopes] = encodeCsv(client.allowedScopes)
            row[redirectUris] = encodeCsv(client.redirectUris)
            row[isConfidential] = client.isConfidential
            row[enabled] = client.enabled
            row[createdAt] = client.createdAt
            row[updatedAt] = client.updatedAt
        }
        client
    }

    override suspend fun update(client: Client): Client = dbQuery {
        ClientsTable.update({ ClientsTable.id eq client.id }) { row ->
            row[clientId] = client.clientId
            row[clientSecretHash] = client.clientSecretHash
            row[name] = client.name
            row[allowedAudiences] = encodeCsv(client.allowedAudiences)
            row[allowedScopes] = encodeCsv(client.allowedScopes)
            row[redirectUris] = encodeCsv(client.redirectUris)
            row[isConfidential] = client.isConfidential
            row[enabled] = client.enabled
            row[updatedAt] = client.updatedAt
        }
        client
    }

    private fun ResultRow.toClient() = Client(
        id = this[ClientsTable.id],
        clientId = this[ClientsTable.clientId],
        clientSecretHash = this[ClientsTable.clientSecretHash],
        name = this[ClientsTable.name],
        allowedAudiences = decodeCsv(this[ClientsTable.allowedAudiences]),
        allowedScopes = decodeCsv(this[ClientsTable.allowedScopes]),
        redirectUris = decodeCsv(this[ClientsTable.redirectUris]),
        isConfidential = this[ClientsTable.isConfidential],
        enabled = this[ClientsTable.enabled],
        createdAt = this[ClientsTable.createdAt],
        updatedAt = this[ClientsTable.updatedAt],
    )
}

class AuditLogRepositoryImpl : AuditLogRepository {
    override suspend fun append(log: AuditLog) {
        dbQuery {
            AuditLogsTable.insert { row ->
                row[id] = log.id
                row[action] = log.action.name
                row[userId] = log.userId
                row[clientId] = log.clientId
                row[ipAddress] = log.ipAddress
                row[userAgent] = log.userAgent
                row[metadata] = json.encodeToString(log.metadata)
                row[success] = log.success
                row[createdAt] = log.createdAt
            }
        }
    }

    override suspend fun list(
        request: PageRequest,
        action: AuditAction?,
        userId: Uuid?,
    ): PageResult<AuditLog> = dbQuery {
        var condition: Op<Boolean> = Op.TRUE
        action?.let { condition = condition and (AuditLogsTable.action eq it.name) }
        userId?.let { condition = condition and (AuditLogsTable.userId eq it) }

        val query = AuditLogsTable.selectAll().where { condition }
        val total = query.count()
        val items = query
            .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
            .limit(request.limit).offset(request.offset)
            .map { it.toAuditLog() }
        PageResult(items, request.page, request.size, total)
    }

    private fun ResultRow.toAuditLog(): AuditLog {
        val meta = runCatching {
            json.decodeFromString<Map<String, String>>(this[AuditLogsTable.metadata])
        }.getOrElse { emptyMap() }
        return AuditLog(
            id = this[AuditLogsTable.id],
            action = AuditAction.valueOf(this[AuditLogsTable.action]),
            userId = this[AuditLogsTable.userId],
            clientId = this[AuditLogsTable.clientId],
            ipAddress = this[AuditLogsTable.ipAddress],
            userAgent = this[AuditLogsTable.userAgent],
            metadata = meta,
            success = this[AuditLogsTable.success],
            createdAt = this[AuditLogsTable.createdAt],
        )
    }
}

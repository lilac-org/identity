package id.andreasmlbngaol.identity.data.repository

import id.andreasmlbngaol.identity.data.db.EmailVerificationTokensTable
import id.andreasmlbngaol.identity.data.db.PasswordResetTokensTable
import id.andreasmlbngaol.identity.data.db.RefreshTokensTable
import id.andreasmlbngaol.identity.data.db.dbQuery
import id.andreasmlbngaol.identity.domain.model.RefreshToken
import id.andreasmlbngaol.identity.domain.model.VerificationToken
import id.andreasmlbngaol.identity.domain.repository.EmailVerificationTokenRepository
import id.andreasmlbngaol.identity.domain.repository.PasswordResetTokenRepository
import id.andreasmlbngaol.identity.domain.repository.RefreshTokenRepository
import id.andreasmlbngaol.identity.domain.service.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

class RefreshTokenRepositoryImpl(
    private val clock: Clock,
) : RefreshTokenRepository {

    override suspend fun create(token: RefreshToken): RefreshToken = dbQuery {
        RefreshTokensTable.insert { row ->
            row[id] = token.id
            row[userId] = token.userId
            row[familyId] = token.familyId
            row[tokenHash] = token.tokenHash
            row[clientId] = token.clientId
            row[issuedAt] = token.issuedAt
            row[expiresAt] = token.expiresAt
            row[usedAt] = token.usedAt
            row[revokedAt] = token.revokedAt
            row[replacedByTokenId] = token.replacedByTokenId
            row[userAgent] = token.userAgent
            row[ipAddress] = token.ipAddress
        }
        token
    }

    override suspend fun findByHash(tokenHash: String): RefreshToken? = dbQuery {
        RefreshTokensTable.selectAll().where { RefreshTokensTable.tokenHash eq tokenHash }
            .firstOrNull()?.toRefreshToken()
    }

    override suspend fun markUsed(id: Uuid, replacedByTokenId: Uuid) {
        dbQuery {
            RefreshTokensTable.update({ RefreshTokensTable.id eq id }) {
                it[usedAt] = clock.now()
                it[RefreshTokensTable.replacedByTokenId] = replacedByTokenId
            }
        }
    }

    override suspend fun revoke(id: Uuid) {
        dbQuery {
            RefreshTokensTable.update({
                (RefreshTokensTable.id eq id) and RefreshTokensTable.revokedAt.isNull()
            }) { it[revokedAt] = clock.now() }
        }
    }

    override suspend fun revokeFamily(familyId: Uuid) {
        dbQuery {
            RefreshTokensTable.update({
                (RefreshTokensTable.familyId eq familyId) and RefreshTokensTable.revokedAt.isNull()
            }) { it[revokedAt] = clock.now() }
        }
    }

    override suspend fun revokeAllForUser(userId: Uuid) {
        dbQuery {
            RefreshTokensTable.update({
                (RefreshTokensTable.userId eq userId) and RefreshTokensTable.revokedAt.isNull()
            }) { it[revokedAt] = clock.now() }
        }
    }

    override suspend fun deleteExpired(): Int = dbQuery {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.expiresAt less clock.now() }
    }

    private fun ResultRow.toRefreshToken() = RefreshToken(
        id = this[RefreshTokensTable.id],
        userId = this[RefreshTokensTable.userId],
        familyId = this[RefreshTokensTable.familyId],
        tokenHash = this[RefreshTokensTable.tokenHash],
        clientId = this[RefreshTokensTable.clientId],
        issuedAt = this[RefreshTokensTable.issuedAt],
        expiresAt = this[RefreshTokensTable.expiresAt],
        usedAt = this[RefreshTokensTable.usedAt],
        revokedAt = this[RefreshTokensTable.revokedAt],
        replacedByTokenId = this[RefreshTokensTable.replacedByTokenId],
        userAgent = this[RefreshTokensTable.userAgent],
        ipAddress = this[RefreshTokensTable.ipAddress],
    )
}

class EmailVerificationTokenRepositoryImpl(private val clock: Clock) :
    EmailVerificationTokenRepository {
    override suspend fun create(token: VerificationToken): VerificationToken = dbQuery {
        EmailVerificationTokensTable.insert { row ->
            row[id] = token.id
            row[userId] = token.userId
            row[tokenHash] = token.tokenHash
            row[expiresAt] = token.expiresAt
            row[usedAt] = token.usedAt
            row[createdAt] = token.createdAt
        }
        token
    }

    override suspend fun findByHash(tokenHash: String): VerificationToken? = dbQuery {
        EmailVerificationTokensTable.selectAll().where { EmailVerificationTokensTable.tokenHash eq tokenHash }
            .firstOrNull()?.let {
                VerificationToken(
                    id = it[EmailVerificationTokensTable.id],
                    userId = it[EmailVerificationTokensTable.userId],
                    tokenHash = it[EmailVerificationTokensTable.tokenHash],
                    expiresAt = it[EmailVerificationTokensTable.expiresAt],
                    usedAt = it[EmailVerificationTokensTable.usedAt],
                    createdAt = it[EmailVerificationTokensTable.createdAt],
                )
            }
    }

    override suspend fun markUsed(id: Uuid) {
        dbQuery {
            EmailVerificationTokensTable.update({ EmailVerificationTokensTable.id eq id }) {
                it[usedAt] = clock.now()
            }
        }
    }

    override suspend fun invalidateAllForUser(userId: Uuid) {
        dbQuery {
            EmailVerificationTokensTable.update({
                (EmailVerificationTokensTable.userId eq userId) and EmailVerificationTokensTable.usedAt.isNull()
            }) { it[usedAt] = clock.now() }
        }
    }
}

class PasswordResetTokenRepositoryImpl(private val clock: Clock) :
    PasswordResetTokenRepository {
    override suspend fun create(token: VerificationToken): VerificationToken = dbQuery {
        PasswordResetTokensTable.insert { row ->
            row[id] = token.id
            row[userId] = token.userId
            row[tokenHash] = token.tokenHash
            row[expiresAt] = token.expiresAt
            row[usedAt] = token.usedAt
            row[createdAt] = token.createdAt
        }
        token
    }

    override suspend fun findByHash(tokenHash: String): VerificationToken? = dbQuery {
        PasswordResetTokensTable.selectAll().where { PasswordResetTokensTable.tokenHash eq tokenHash }
            .firstOrNull()?.let {
                VerificationToken(
                    id = it[PasswordResetTokensTable.id],
                    userId = it[PasswordResetTokensTable.userId],
                    tokenHash = it[PasswordResetTokensTable.tokenHash],
                    expiresAt = it[PasswordResetTokensTable.expiresAt],
                    usedAt = it[PasswordResetTokensTable.usedAt],
                    createdAt = it[PasswordResetTokensTable.createdAt],
                )
            }
    }

    override suspend fun markUsed(id: Uuid) {
        dbQuery {
            PasswordResetTokensTable.update({ PasswordResetTokensTable.id eq id }) {
                it[usedAt] = clock.now()
            }
        }
    }

    override suspend fun invalidateAllForUser(userId: Uuid) {
        dbQuery {
            PasswordResetTokensTable.update({
                (PasswordResetTokensTable.userId eq userId) and PasswordResetTokensTable.usedAt.isNull()
            }) { it[usedAt] = clock.now() }
        }
    }
}

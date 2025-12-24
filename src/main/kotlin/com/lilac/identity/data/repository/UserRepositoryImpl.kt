package com.lilac.identity.data.repository

import com.lilac.identity.data.mapper.toDomain
import com.lilac.identity.data.model.UserEntity
import com.lilac.identity.db.tables.UsersTable
import com.lilac.identity.domain.model.CreateUserException
import com.lilac.identity.domain.model.User
import com.lilac.identity.domain.repository.UserRepository
import com.lilac.identity.util.toUUID
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.orWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class UserRepositoryImpl(): UserRepository {
    private fun ResultRow.toEntity() = UserEntity(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        username = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        isEmailVerified = this[UsersTable.isEmailVerified],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )

    private fun UsersTable.updateTimestamp(
        where: () -> Op<Boolean>,
        body: UsersTable.(UpdateStatement) -> Unit
    ) = this
        .update(where) {
            it[updatedAt] = System.currentTimeMillis()
            body(it)
        }

    override fun findById(id: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id.toUUID() }
            .map { it.toEntity() }
            .singleOrNull()
            ?.toDomain()
    }

    override fun findByEmail(email: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .map { it.toEntity() }
            .singleOrNull()
            ?.toDomain()
    }

    override fun findByEmailOrUsername(emailOrUsername: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq emailOrUsername }
            .orWhere { UsersTable.username eq emailOrUsername }
            .map { it.toEntity() }
            .singleOrNull()
            ?.toDomain()
    }

    override fun existsByEmail(email: String): Boolean = transaction {
        UsersTable
            .select(UsersTable.id)
            .where { UsersTable.email eq email }
            .count() > 0
    }

    override fun existsByUsername(username: String): Boolean = transaction {
        UsersTable
            .select(UsersTable.id)
            .where { UsersTable.username eq username }
            .count() > 0
    }

    override fun create(
        email: String,
        username: String,
        passwordHash: String,
        firstName: String,
        lastName: String,
        isEmailVerified: Boolean
    ): String = transaction {
        try {
            val now = System.currentTimeMillis()
            UsersTable
                .insertAndGetId {
                    it[UsersTable.email] = email
                    it[UsersTable.username] = username
                    it[UsersTable.passwordHash] = passwordHash
                    it[UsersTable.firstName] = firstName
                    it[UsersTable.lastName] = lastName
                    it[UsersTable.isEmailVerified] = isEmailVerified
                    it[UsersTable.createdAt] = now
                    it[UsersTable.updatedAt] = now
                }
                .value
                .toString()
        } catch (e: Exception) {
            throw CreateUserException("Failed to create user: ${e.message}")
        }
    }

    override fun deleteById(id: String): Boolean = transaction {
        UsersTable
            .deleteWhere {
                UsersTable.id eq id.toUUID()
            } > 0
    }

    override fun updatePassword(id: String, newHash: String): Boolean = transaction {
        UsersTable
            .updateTimestamp({ UsersTable.id eq id.toUUID() }) {
                it[UsersTable.passwordHash] = newHash
            } > 0
    }

    override fun markEmailVerified(id: String): Boolean = transaction {
        UsersTable
            .updateTimestamp({ UsersTable.id eq id.toUUID() }) {
                it[UsersTable.isEmailVerified] = true
            } > 0
    }
}
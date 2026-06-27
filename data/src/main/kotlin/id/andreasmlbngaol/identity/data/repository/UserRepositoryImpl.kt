package id.andreasmlbngaol.identity.data.repository

import id.andreasmlbngaol.identity.data.db.PermissionsTable
import id.andreasmlbngaol.identity.data.db.RolePermissionsTable
import id.andreasmlbngaol.identity.data.db.RolesTable
import id.andreasmlbngaol.identity.data.db.UserRolesTable
import id.andreasmlbngaol.identity.data.db.UsersTable
import id.andreasmlbngaol.identity.data.db.dbQuery
import id.andreasmlbngaol.identity.domain.model.PageRequest
import id.andreasmlbngaol.identity.domain.model.PageResult
import id.andreasmlbngaol.identity.domain.model.Permission
import id.andreasmlbngaol.identity.domain.model.Role
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

class UserRepositoryImpl : UserRepository {

    override suspend fun create(user: User): User = dbQuery {
        UsersTable.insert { row ->
            row[id] = user.id
            row[email] = user.email
            row[username] = user.username
            row[passwordHash] = user.passwordHash
            row[status] = user.status.name
            row[emailVerified] = user.emailVerified
            row[fullName] = user.fullName
            row[photoUrl] = user.photoUrl
            row[phoneNumber] = user.phoneNumber
            row[phoneVerified] = user.phoneVerified
            row[dateOfBirth] = user.dateOfBirth
            row[tokenVersion] = user.tokenVersion
            row[createdAt] = user.createdAt
            row[updatedAt] = user.updatedAt
            row[deletedAt] = user.deletedAt
        }
        user
    }

    override suspend fun update(user: User): User = dbQuery {
        UsersTable.update({ UsersTable.id eq user.id }) { row ->
            row[email] = user.email
            row[username] = user.username
            row[passwordHash] = user.passwordHash
            row[status] = user.status.name
            row[emailVerified] = user.emailVerified
            row[fullName] = user.fullName
            row[photoUrl] = user.photoUrl
            row[phoneNumber] = user.phoneNumber
            row[phoneVerified] = user.phoneVerified
            row[dateOfBirth] = user.dateOfBirth
            row[tokenVersion] = user.tokenVersion
            row[updatedAt] = user.updatedAt
            row[deletedAt] = user.deletedAt
        }
        loadById(user.id) ?: user
    }

    override suspend fun findById(id: Uuid): User? = dbQuery { loadById(id) }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email }.firstOrNull()
            ?.let { it.toUser(loadRoles(it[UsersTable.id])) }
    }

    override suspend fun findByUsername(username: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.username eq username }.firstOrNull()
            ?.let { it.toUser(loadRoles(it[UsersTable.id])) }
    }

    override suspend fun findByPhoneNumber(phone: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.phoneNumber eq phone }.firstOrNull()
            ?.let { it.toUser(loadRoles(it[UsersTable.id])) }
    }

    override suspend fun findByAnyIdentifier(identifier: String): User? = dbQuery {
        val normalized = identifier.lowercase()
        UsersTable.selectAll().where {
            (UsersTable.email.lowerCase() eq normalized) or
                (UsersTable.username eq identifier) or
                (UsersTable.phoneNumber eq identifier)
        }.firstOrNull()?.let { it.toUser(loadRoles(it[UsersTable.id])) }
    }

    override suspend fun existsByEmail(email: String): Boolean = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email }.limit(1).any()
    }

    override suspend fun existsByUsername(username: String): Boolean = dbQuery {
        UsersTable.selectAll().where { UsersTable.username eq username }.limit(1).any()
    }

    override suspend fun existsByPhoneNumber(phone: String): Boolean = dbQuery {
        UsersTable.selectAll().where { UsersTable.phoneNumber eq phone }.limit(1).any()
    }

    override suspend fun incrementTokenVersion(id: Uuid): Int = dbQuery {
        val current = UsersTable.selectAll().where { UsersTable.id eq id }
            .firstOrNull()?.get(UsersTable.tokenVersion) ?: 0
        val next = current + 1
        UsersTable.update({ UsersTable.id eq id }) { it[tokenVersion] = next }
        next
    }

    override suspend fun list(request: PageRequest): PageResult<User> = dbQuery {
        val base = UsersTable.selectAll()
        val filtered = request.search?.takeIf { it.isNotBlank() }?.let { term ->
            val like = "%${term.lowercase()}%"
            base.where {
                (UsersTable.email.lowerCase() like like) or
                    (UsersTable.username.lowerCase() like like) or
                    (UsersTable.fullName.lowerCase() like like)
            }
        } ?: base
        val total = filtered.count()
        val items = filtered
            .orderBy(UsersTable.createdAt)
            .limit(request.limit).offset(request.offset)
            .map { it.toUser(loadRoles(it[UsersTable.id])) }
        PageResult(items, request.page, request.size, total)
    }

    private fun loadById(id: Uuid): User? =
        UsersTable.selectAll().where { UsersTable.id eq id }.firstOrNull()
            ?.let { it.toUser(loadRoles(id)) }

    /** Loads the user's roles with their permissions in two grouped queries. */
    private fun loadRoles(userId: Uuid): Set<Role> {
        val roleRows = (UserRolesTable innerJoin RolesTable)
            .selectAll().where { UserRolesTable.userId eq userId }
            .toList()
        if (roleRows.isEmpty()) return emptySet()

        return roleRows.map { row ->
            val roleId = row[RolesTable.id]
            val permissions = (RolePermissionsTable innerJoin PermissionsTable)
                .selectAll().where { RolePermissionsTable.roleId eq roleId }
                .map { p ->
                    Permission(
                        id = p[PermissionsTable.id],
                        name = p[PermissionsTable.name],
                        description = p[PermissionsTable.description],
                    )
                }.toSet()
            Role(
                id = roleId,
                name = row[RolesTable.name],
                description = row[RolesTable.description],
                isSystem = row[RolesTable.isSystem],
                permissions = permissions,
            )
        }.toSet()
    }
}

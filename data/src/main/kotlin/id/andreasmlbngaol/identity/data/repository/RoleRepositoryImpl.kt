package id.andreasmlbngaol.identity.data.repository

import id.andreasmlbngaol.identity.data.db.PermissionsTable
import id.andreasmlbngaol.identity.data.db.RolePermissionsTable
import id.andreasmlbngaol.identity.data.db.RolesTable
import id.andreasmlbngaol.identity.data.db.UserRolesTable
import id.andreasmlbngaol.identity.data.db.dbQuery
import id.andreasmlbngaol.identity.domain.model.Permission
import id.andreasmlbngaol.identity.domain.model.Role
import id.andreasmlbngaol.identity.domain.repository.RoleRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.uuid.Uuid

class RoleRepositoryImpl : RoleRepository {

    override suspend fun findByName(name: String): Role? = dbQuery {
        RolesTable.selectAll().where { RolesTable.name eq name }.firstOrNull()?.toRole()
    }

    override suspend fun findById(id: Uuid): Role? = dbQuery {
        RolesTable.selectAll().where { RolesTable.id eq id }.firstOrNull()?.toRole()
    }

    override suspend fun findAll(): List<Role> = dbQuery {
        RolesTable.selectAll().orderBy(RolesTable.name).map { it.toRole() }
    }

    override suspend fun create(role: Role): Role = dbQuery {
        RolesTable.insert { row ->
            row[id] = role.id
            row[name] = role.name
            row[description] = role.description
            row[isSystem] = role.isSystem
        }
        role.permissions.forEach { permission ->
            RolePermissionsTable.insertIgnore {
                it[roleId] = role.id
                it[permissionId] = permission.id
            }
        }
        role
    }

    override suspend fun delete(id: Uuid) {
        dbQuery {
            RolePermissionsTable.deleteWhere { RolePermissionsTable.roleId eq id }
            UserRolesTable.deleteWhere { UserRolesTable.roleId eq id }
            RolesTable.deleteWhere { RolesTable.id eq id }
        }
    }

    override suspend fun assignRoleToUser(userId: Uuid, roleId: Uuid) {
        dbQuery {
            UserRolesTable.insertIgnore {
                it[UserRolesTable.userId] = userId
                it[UserRolesTable.roleId] = roleId
            }
        }
    }

    override suspend fun revokeRoleFromUser(userId: Uuid, roleId: Uuid) {
        dbQuery {
            UserRolesTable.deleteWhere {
                (UserRolesTable.userId eq userId) and (UserRolesTable.roleId eq roleId)
            }
        }
    }

    override suspend fun findAllPermissions(): List<Permission> = dbQuery {
        PermissionsTable.selectAll().orderBy(PermissionsTable.name).map { it.toPermission() }
    }

    override suspend fun setRolePermissions(roleId: Uuid, permissionIds: Set<Uuid>) {
        dbQuery {
            RolePermissionsTable.deleteWhere { RolePermissionsTable.roleId eq roleId }
            permissionIds.forEach { permissionId ->
                RolePermissionsTable.insertIgnore {
                    it[RolePermissionsTable.roleId] = roleId
                    it[RolePermissionsTable.permissionId] = permissionId
                }
            }
        }
    }

    private fun ResultRow.toPermission() = Permission(
        id = this[PermissionsTable.id],
        name = this[PermissionsTable.name],
        description = this[PermissionsTable.description],
    )

    private fun ResultRow.toRole(): Role {
        val roleId = this[RolesTable.id]
        val permissions = (RolePermissionsTable innerJoin PermissionsTable)
            .selectAll().where { RolePermissionsTable.roleId eq roleId }
            .map { it.toPermission() }
            .toSet()
        return Role(
            id = roleId,
            name = this[RolesTable.name],
            description = this[RolesTable.description],
            isSystem = this[RolesTable.isSystem],
            permissions = permissions,
        )
    }
}

package id.andreasmlbngaol.identity.domain.repository

import id.andreasmlbngaol.identity.domain.model.Permission
import id.andreasmlbngaol.identity.domain.model.Role
import kotlin.uuid.Uuid

interface RoleRepository {
    suspend fun findByName(name: String): Role?
    suspend fun findById(id: Uuid): Role?
    suspend fun findAll(): List<Role>
    suspend fun create(role: Role): Role
    suspend fun delete(id: Uuid)

    suspend fun assignRoleToUser(userId: Uuid, roleId: Uuid)
    suspend fun revokeRoleFromUser(userId: Uuid, roleId: Uuid)

    suspend fun findAllPermissions(): List<Permission>
    suspend fun setRolePermissions(roleId: Uuid, permissionIds: Set<Uuid>)
}

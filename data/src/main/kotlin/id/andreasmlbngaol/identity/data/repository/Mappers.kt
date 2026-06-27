package id.andreasmlbngaol.identity.data.repository

import id.andreasmlbngaol.identity.data.db.UsersTable
import id.andreasmlbngaol.identity.domain.enums.UserStatus
import id.andreasmlbngaol.identity.domain.model.Role
import id.andreasmlbngaol.identity.domain.model.User
import org.jetbrains.exposed.v1.core.ResultRow

/** Row -> domain mappers. Kept internal so persistence types never leak out. */
internal fun ResultRow.toUser(roles: Set<Role> = emptySet()): User = User(
    id = this[UsersTable.id],
    email = this[UsersTable.email],
    username = this[UsersTable.username],
    passwordHash = this[UsersTable.passwordHash],
    status = UserStatus.valueOf(this[UsersTable.status]),
    emailVerified = this[UsersTable.emailVerified],
    fullName = this[UsersTable.fullName],
    photoUrl = this[UsersTable.photoUrl],
    phoneNumber = this[UsersTable.phoneNumber],
    phoneVerified = this[UsersTable.phoneVerified],
    dateOfBirth = this[UsersTable.dateOfBirth],
    tokenVersion = this[UsersTable.tokenVersion],
    roles = roles,
    createdAt = this[UsersTable.createdAt],
    updatedAt = this[UsersTable.updatedAt],
    deletedAt = this[UsersTable.deletedAt],
)

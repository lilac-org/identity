package id.andreasmlbngaol.identity.domain.repository

import id.andreasmlbngaol.identity.domain.model.PageRequest
import id.andreasmlbngaol.identity.domain.model.PageResult
import id.andreasmlbngaol.identity.domain.model.User
import kotlin.uuid.Uuid

interface UserRepository {
    suspend fun create(user: User): User
    suspend fun update(user: User): User
    suspend fun findById(id: Uuid): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findByUsername(username: String): User?
    suspend fun findByPhoneNumber(phone: String): User?

    /** Resolves a login identifier that may be an email, username, or phone. */
    suspend fun findByAnyIdentifier(identifier: String): User?

    suspend fun existsByEmail(email: String): Boolean
    suspend fun existsByUsername(username: String): Boolean
    suspend fun existsByPhoneNumber(phone: String): Boolean

    /** Atomically bumps the token version (used to invalidate all tokens). */
    suspend fun incrementTokenVersion(id: Uuid): Int

    suspend fun list(request: PageRequest): PageResult<User>
}

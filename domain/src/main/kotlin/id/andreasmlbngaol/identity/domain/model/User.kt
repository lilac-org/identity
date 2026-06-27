package id.andreasmlbngaol.identity.domain.model

import id.andreasmlbngaol.identity.domain.enums.UserStatus
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

/**
 * The aggregate root for an identity in the system. There is a single shared
 * user pool across every consuming application; per-application authorization
 * is expressed through roles/permissions and client audiences rather than by
 * duplicating users.
 *
 * Optional profile fields are nullable so a freshly registered account only
 * requires the bare minimum (an identifier + password hash, or a linked OAuth
 * account).
 */
data class User(
    val id: Uuid,
    val email: String,
    val username: String,
    /** Argon2id hash. Null when the account is OAuth-only. */
    val passwordHash: String? = null,
    val status: UserStatus = UserStatus.PENDING_VERIFICATION,
    val emailVerified: Boolean = false,
    // --- Optional profile fields ---
    val fullName: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val phoneVerified: Boolean = false,
    val dateOfBirth: LocalDate? = null,
    // --- Security bookkeeping ---
    /**
     * Incremented to globally invalidate previously issued access/refresh
     * tokens (e.g. on "log out everywhere", password change, or reset).
     * Embedded as a claim so stateless access tokens can be rejected cheaply.
     */
    val tokenVersion: Int = 0,
    val roles: Set<Role> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
) {
    val isActive: Boolean get() = status == UserStatus.ACTIVE
    val isDeleted: Boolean get() = status == UserStatus.DELETED || deletedAt != null

    /** Flattened, de-duplicated permission names across all assigned roles. */
    val permissionNames: Set<String>
        get() = roles.flatMap { role -> role.permissions.map { it.name } }.toSet()

    val roleNames: Set<String> get() = roles.map { it.name }.toSet()

    fun hasPermission(permission: String): Boolean = permission in permissionNames

    fun hasRole(role: String): Boolean = role in roleNames
}

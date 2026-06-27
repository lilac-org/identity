package id.andreasmlbngaol.identity.domain.model

import kotlin.uuid.Uuid

/**
 * A named bundle of [Permission]s. Users are granted roles; the effective
 * permission set of a user is the union of the permissions of their roles.
 */
data class Role(
    val id: Uuid,
    /** Unique machine name, e.g. "ADMIN". */
    val name: String,
    val description: String? = null,
    /** Built-in roles cannot be deleted through the admin API. */
    val isSystem: Boolean = false,
    val permissions: Set<Permission> = emptySet(),
)

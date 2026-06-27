package id.andreasmlbngaol.identity.domain.model

import kotlin.uuid.Uuid

/**
 * A fine-grained capability expressed as `resource:action`, e.g. `user:read`
 * or `user:delete`. Permissions are grouped into [Role]s.
 */
data class Permission(
    val id: Uuid,
    /** Unique machine name, e.g. "user:read". */
    val name: String,
    val description: String? = null,
) {
    val resource: String get() = name.substringBefore(':')
    val action: String get() = name.substringAfter(':')
}

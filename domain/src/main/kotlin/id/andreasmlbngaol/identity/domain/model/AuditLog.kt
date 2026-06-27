package id.andreasmlbngaol.identity.domain.model

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * An append-only security audit record. Persisted to PostgreSQL (a dedicated,
 * monthly-partitioned table) and written asynchronously so it never blocks the
 * request path.
 */
data class AuditLog(
    val id: Uuid,
    val action: AuditAction,
    /** The acting/subject user, when applicable. */
    val userId: Uuid? = null,
    /** The client/application context, when applicable. */
    val clientId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    /** Free-form JSON-encoded contextual metadata. */
    val metadata: Map<String, String> = emptyMap(),
    val success: Boolean = true,
    val createdAt: Instant,
)

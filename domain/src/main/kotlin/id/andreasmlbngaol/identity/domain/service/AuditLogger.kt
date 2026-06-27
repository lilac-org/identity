package id.andreasmlbngaol.identity.domain.service

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.model.RequestContext
import kotlin.uuid.Uuid

/**
 * Fire-and-forget security audit logging. Implementations write asynchronously
 * so the request path is never blocked by audit persistence.
 */
interface AuditLogger {
    fun record(
        action: AuditAction,
        userId: Uuid? = null,
        context: RequestContext? = null,
        success: Boolean = true,
        metadata: Map<String, String> = emptyMap(),
    )
}

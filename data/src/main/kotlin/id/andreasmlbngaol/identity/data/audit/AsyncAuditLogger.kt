package id.andreasmlbngaol.identity.data.audit

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.model.AuditLog
import id.andreasmlbngaol.identity.domain.model.RequestContext
import id.andreasmlbngaol.identity.domain.repository.AuditLogRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}

/**
 * Non-blocking audit logging. Records are pushed onto a buffered channel and
 * persisted by a single background consumer, so the request path never waits on
 * audit writes. We persist to PostgreSQL (a dedicated, monthly-partitioned
 * table) rather than a separate write-heavy store — partitioning + async insert
 * comfortably handles expected volume without the operational cost of an extra
 * datastore.
 */
class AsyncAuditLogger(
    private val repository: AuditLogRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AuditLogger {
    private val channel = Channel<AuditLog>(capacity = 1024)

    init {
        scope.launch {
            for (entry in channel) {
                runCatching { repository.append(entry) }
                    .onFailure { logger.error(it) { "Failed to persist audit log: ${entry.action}" } }
            }
        }
    }

    override fun record(
        action: AuditAction,
        userId: Uuid?,
        context: RequestContext?,
        success: Boolean,
        metadata: Map<String, String>,
    ) {
        val entry = AuditLog(
            id = idGenerator.newId(),
            action = action,
            userId = userId,
            clientId = context?.clientId,
            ipAddress = context?.ipAddress,
            userAgent = context?.userAgent,
            metadata = metadata,
            success = success,
            createdAt = clock.now(),
        )
        val offered = channel.trySend(entry)
        if (offered.isFailure) {
            logger.warn { "Audit channel full; dropping event ${action.name}" }
        }
    }
}

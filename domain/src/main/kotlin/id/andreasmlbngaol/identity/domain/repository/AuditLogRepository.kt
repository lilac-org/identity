package id.andreasmlbngaol.identity.domain.repository

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.model.AuditLog
import id.andreasmlbngaol.identity.domain.model.PageRequest
import id.andreasmlbngaol.identity.domain.model.PageResult
import kotlin.uuid.Uuid

interface AuditLogRepository {
    suspend fun append(log: AuditLog)
    suspend fun list(request: PageRequest, action: AuditAction? = null, userId: Uuid? = null): PageResult<AuditLog>
}

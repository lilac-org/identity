package id.andreasmlbngaol.identity.domain.model

/**
 * Ambient information about the inbound request, passed from the presentation
 * layer into use cases purely as data (no framework types) so that audit logs
 * and refresh-token bookkeeping can record provenance.
 */
data class RequestContext(
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val clientId: String? = null,
)

package id.andreasmlbngaol.identity.domain.model

/**
 * A transport-agnostic outbound email. Lives in the domain model layer so both
 * the email-template service and the concrete senders (data layer) can depend
 * on it without coupling to a particular transport.
 */
data class EmailMessage(
    val to: String,
    val subject: String,
    val htmlBody: String,
    val textBody: String? = null,
)

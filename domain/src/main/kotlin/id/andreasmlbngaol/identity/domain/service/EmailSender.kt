package id.andreasmlbngaol.identity.domain.service

import id.andreasmlbngaol.identity.domain.model.EmailMessage

/**
 * Transport-agnostic outbound email. Implementations: Resend (default), SMTP
 * (Simple Java Mail), and a no-op logging sender for local development.
 */
interface EmailSender {
    suspend fun send(message: EmailMessage)
}

package id.andreasmlbngaol.identity.domain.service

import id.andreasmlbngaol.identity.domain.model.EmailMessage

/**
 * Builds the user-facing transactional emails. Pure string templating so it can
 * live in the domain; the verification/reset links are composed from a base URL
 * supplied by configuration.
 */
class EmailTemplates(private val appName: String) {

    fun verification(to: String, verifyUrl: String): EmailMessage {
        val html = """
            <h2>Verify your email</h2>
            <p>Welcome to $appName! Please confirm your email address by clicking the link below.</p>
            <p><a href="$verifyUrl">Verify my email</a></p>
            <p>If you did not create an account, you can safely ignore this email.</p>
        """.trimIndent()
        return EmailMessage(
            to = to,
            subject = "Verify your email address",
            htmlBody = html,
            textBody = "Verify your email: $verifyUrl",
        )
    }

    fun passwordReset(to: String, resetUrl: String): EmailMessage {
        val html = """
            <h2>Reset your password</h2>
            <p>We received a request to reset your $appName password.</p>
            <p><a href="$resetUrl">Choose a new password</a></p>
            <p>This link expires soon. If you did not request a reset, ignore this email.</p>
        """.trimIndent()
        return EmailMessage(
            to = to,
            subject = "Reset your password",
            htmlBody = html,
            textBody = "Reset your password: $resetUrl",
        )
    }
}

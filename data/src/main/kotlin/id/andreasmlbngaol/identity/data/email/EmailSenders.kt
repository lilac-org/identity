package id.andreasmlbngaol.identity.data.email

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import id.andreasmlbngaol.identity.data.config.EmailConfig
import id.andreasmlbngaol.identity.domain.model.EmailMessage
import id.andreasmlbngaol.identity.domain.service.EmailSender
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

private val logger = KotlinLogging.logger {}

/** Dev-friendly sender: logs the message instead of sending anything. */
class LogEmailSender : EmailSender {
    override suspend fun send(message: EmailMessage) {
        logger.info { "[DEV EMAIL] to=${message.to} subject='${message.subject}'\n${message.textBody ?: message.htmlBody}" }
    }
}

/** Production default — Resend transactional email API. */
class ResendEmailSender(private val config: EmailConfig) : EmailSender {
    private val client = Resend(requireNotNull(config.resendApiKey) { "resendApiKey is required for the Resend sender" })

    override suspend fun send(message: EmailMessage) = withContext(Dispatchers.IO) {
        val options = CreateEmailOptions.builder()
            .from("${config.fromName} <${config.fromAddress}>")
            .to(message.to)
            .subject(message.subject)
            .html(message.htmlBody)
            .apply { message.textBody?.let { text(it) } }
            .build()
        client.emails().send(options)
        Unit
    }
}

/** Classic SMTP via Simple Java Mail. */
class SmtpEmailSender(private val config: EmailConfig) : EmailSender {
    private val mailer = MailerBuilder
        .withSMTPServer(config.smtpHost, config.smtpPort, config.smtpUsername, config.smtpPassword)
        .buildMailer()

    override suspend fun send(message: EmailMessage) = withContext(Dispatchers.IO) {
        val email = EmailBuilder.startingBlank()
            .from(config.fromName, config.fromAddress)
            .to(message.to)
            .withSubject(message.subject)
            .withHTMLText(message.htmlBody)
            .apply { message.textBody?.let { withPlainText(it) } }
            .buildEmail()
        mailer.sendMail(email)
        Unit
    }
}

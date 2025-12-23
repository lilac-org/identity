package com.lilac.identity.data.service

import com.lilac.identity.config.MailConfig
import com.lilac.identity.domain.service.MailService
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

class MailServiceImpl(
    private val config: MailConfig
): MailService {
    override fun send(to: String, subject: String, html: String) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port)
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(config.username, config.password)
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.from))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject)
            setContent(html, "text/html; charset=utf-8")
        }

        Transport.send(message)
    }
}
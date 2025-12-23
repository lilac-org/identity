package com.lilac.identity.config

import io.ktor.server.application.Application

data class MailConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val from: String
)

fun Application.loadMailConfig(): MailConfig {
    val cfg = environment.config

    val host = cfg.propertyOrNull("mail.host")?.getString() ?: error("Mail Host must be specified")
    val port = cfg.propertyOrNull("mail.port")?.getString()?.toInt() ?: 587
    val username = cfg.propertyOrNull("mail.username")?.getString() ?: error("Mail Username must be specified")
    val password = cfg.propertyOrNull("mail.password")?.getString() ?: error("Mail Password must be specified")
    val from = cfg.propertyOrNull("mail.from")?.getString() ?: error("Mail From must be specified")

    return MailConfig(
        host = host,
        port = port,
        username = username,
        password = password,
        from = from
    )
}
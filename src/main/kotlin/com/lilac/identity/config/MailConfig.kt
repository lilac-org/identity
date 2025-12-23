package com.lilac.identity.config

import com.lilac.identity.util.getConfig
import io.ktor.server.application.Application

data class MailConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val from: String
)

fun Application.loadMailConfig(): MailConfig {
    val host = getConfig("mail.host") ?: error("Mail Host must be specified")
    val port = getConfig("mail.port")?.toInt() ?: 587
    val username = getConfig("mail.username") ?: error("Mail Username must be specified")
    val password = getConfig("mail.password") ?: error("Mail Password must be specified")
    val from = getConfig("mail.from") ?: error("Mail From must be specified")

    return MailConfig(
        host = host,
        port = port,
        username = username,
        password = password,
        from = from
    )
}
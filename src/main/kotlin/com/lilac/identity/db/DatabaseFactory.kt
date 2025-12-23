package com.lilac.identity.db

import com.lilac.identity.util.getConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun Application.configureDatabase() {
        val jdbcUrl = getConfig("postgres.url") ?: error("Missing postgres.url")
        val username = getConfig("postgres.username") ?: error("Missing postgres.username")
        val password = getConfig("postgres.password") ?: error("Missing postgres.password")
        val maximumPoolSize = getConfig("postgres.maximumPoolSize")?.toInt() ?: 10


        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.maximumPoolSize = maximumPoolSize
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        Database.Companion.connect(HikariDataSource(config))

        createTables()
    }

    fun createTables() = transaction {
//        SchemaUtils.create()
    }
}
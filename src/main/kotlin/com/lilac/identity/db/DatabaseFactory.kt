package com.lilac.identity.db

import com.lilac.identity.db.tables.UserProfilesTable
import com.lilac.identity.db.tables.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun Application.configureDatabase() {
        val cfg = environment.config

        val jdbcUrl = cfg.propertyOrNull("postgres.url")?.getString() ?: error("Missing postgres.url")
        val username = cfg.propertyOrNull("postgres.username")?.getString() ?: error("Missing postgres.username")
        val password = cfg.propertyOrNull("postgres.password")?.getString() ?: error("Missing postgres.password")
        val maximumPoolSize = cfg.propertyOrNull("datasource.maximumPoolSize")?.getString()?.toInt() ?: 10

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.maximumPoolSize = maximumPoolSize
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        Database.connect(HikariDataSource(config))

        createTables()
    }

    fun createTables() = transaction {
        SchemaUtils.create(
            UsersTable,
            UserProfilesTable
        )
    }
}
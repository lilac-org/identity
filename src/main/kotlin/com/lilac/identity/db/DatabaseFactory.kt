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

        val jdbcUrl = cfg.propertyOrNull("ktor.datasource.jdbcUrl")?.getString() ?: error("Missing datasource.jdbcUrl")
        val username = cfg.propertyOrNull("ktor.datasource.username")?.getString() ?: error("Missing datasource.username")
        val password = cfg.propertyOrNull("ktor.datasource.password")?.getString() ?: error("Missing datasource.password")
        val maximumPoolSize = cfg.propertyOrNull("ktor.datasource.maximumPoolSize")?.getString()?.toInt() ?: 10

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
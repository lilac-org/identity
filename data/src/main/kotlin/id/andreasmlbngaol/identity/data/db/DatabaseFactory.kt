package id.andreasmlbngaol.identity.data.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import id.andreasmlbngaol.identity.data.config.DatabaseConfig
import id.andreasmlbngaol.identity.domain.service.TransactionRunner
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import javax.sql.DataSource

/**
 * Owns the HikariCP connection pool, runs Flyway migrations on startup, and
 * exposes a single coroutine-friendly query helper. Why a pool? Opening a
 * PostgreSQL connection is expensive (TCP + auth + backend process); the pool
 * keeps a small set of warm connections to reuse, bounding load on the DB and
 * keeping request latency low.
 *
 * `connect()` calls [Database.connect], which registers the database as the
 * default for Exposed's transaction manager — that is why [dbQuery] and
 * [ExposedTransactionRunner] can call `suspendTransaction { }` without passing a
 * `Database` explicitly.
 */
class DatabaseFactory(private val config: DatabaseConfig) {

    private lateinit var pool: HikariDataSource
    lateinit var database: Database
        private set

    fun connect() {
        pool = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.jdbcUrl
                username = config.username
                password = config.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = config.maxPoolSize
                minimumIdle = config.minIdle
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                poolName = "identity-pool"
            },
        )
        if (config.runMigrationsOnStart) migrate(pool)
        database = Database.connect(pool)
    }

    /** The underlying pooled [DataSource] (exposed for diagnostics/tests). */
    fun dataSource(): DataSource = pool

    fun close() {
        if (::pool.isInitialized) pool.close()
    }

    private fun migrate(ds: DataSource) {
        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }
}

/**
 * Exposed-backed [TransactionRunner]. Repositories funnel queries through
 * `suspendTransaction` so that grouping several operations into one atomic unit
 * is simply nesting them inside [inTransaction].
 */
class ExposedTransactionRunner : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        suspendTransaction { block() }
}

/** Convenience used by repositories for single-statement queries. */
suspend fun <T> dbQuery(block: suspend () -> T): T =
    suspendTransaction { block() }

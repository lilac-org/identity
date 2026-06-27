package id.andreasmlbngaol.identity.data.db

import id.andreasmlbngaol.identity.data.config.DatabaseConfig
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

/**
 * Integration test that boots a real PostgreSQL via Testcontainers, lets the
 * [DatabaseFactory] run the Flyway migrations, and asserts the schema and seed
 * data were applied. Requires a working Docker daemon.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationIntegrationTest {

    private val postgres = PostgreSQLContainer(
        DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres"),
    ).withDatabaseName("identity").withUsername("identity").withPassword("identity")

    private lateinit var factory: DatabaseFactory

    private fun start() {
        postgres.start()
        factory = DatabaseFactory(
            DatabaseConfig(
                jdbcUrl = postgres.jdbcUrl,
                username = postgres.username,
                password = postgres.password,
                runMigrationsOnStart = true,
            ),
        )
        factory.connect()
    }

    @AfterAll
    fun tearDown() {
        if (::factory.isInitialized) factory.close()
        if (postgres.isRunning) postgres.stop()
    }

    @Test
    fun `migrations create schema and seed default roles`() {
        start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM roles").use { rs ->
                    rs.next()
                    rs.getInt(1) shouldBe 2
                }
                st.executeQuery("SELECT count(*) FROM permissions").use { rs ->
                    rs.next()
                    rs.getInt(1) shouldBe 6
                }
                // ADMIN must have every permission granted.
                st.executeQuery(
                    """
                    SELECT count(*) FROM role_permissions rp
                    JOIN roles r ON r.id = rp.role_id
                    WHERE r.name = 'ADMIN'
                    """.trimIndent(),
                ).use { rs ->
                    rs.next()
                    rs.getInt(1) shouldBe 6
                }
                // The audit_logs partitioned table must exist and be writable.
                st.executeQuery("SELECT count(*) FROM audit_logs").use { rs ->
                    rs.next()
                    rs.getInt(1) shouldBe 0
                }
            }
        }
    }
}

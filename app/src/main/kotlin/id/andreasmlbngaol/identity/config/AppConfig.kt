package id.andreasmlbngaol.identity.config

import id.andreasmlbngaol.identity.data.config.DatabaseConfig
import id.andreasmlbngaol.identity.data.config.EmailConfig
import id.andreasmlbngaol.identity.data.config.JwtKeyConfig
import id.andreasmlbngaol.identity.data.config.OAuthConfig
import id.andreasmlbngaol.identity.data.config.OAuthProviderConfig
import id.andreasmlbngaol.identity.data.config.RateLimitConfig
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import io.github.cdimascio.dotenv.dotenv
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Fully-resolved, immutable application configuration. */
data class AppConfig(
    val server: ServerConfig,
    val appName: String,
    val frontendBaseUrl: String,
    val corsAllowedHosts: List<String>,
    val swaggerEnabled: Boolean,
    val adminDashboardEnabled: Boolean,
    val database: DatabaseConfig,
    val jwtKeys: JwtKeyConfig,
    val email: EmailConfig,
    val oauth: OAuthConfig,
    val rateLimit: RateLimitConfig,
    val policy: AuthPolicy,
)

data class ServerConfig(val host: String, val port: Int)

/**
 * Loads configuration from environment variables (12-factor), transparently
 * sourcing a local `.env` file in development via dotenv-kotlin. Secrets such
 * as DB credentials and RSA keys are never hard-coded.
 */
object ConfigLoader {

    private val dotenv = dotenv {
        ignoreIfMissing = true
        directory = System.getProperty("dotenv.dir", "./")
    }

    private fun get(key: String): String? =
        System.getenv(key) ?: dotenv[key]

    private fun get(key: String, default: String): String = get(key) ?: default
    private fun int(key: String, default: Int): Int = get(key)?.toIntOrNull() ?: default
    private fun long(key: String, default: Long): Long = get(key)?.toLongOrNull() ?: default
    private fun bool(key: String, default: Boolean): Boolean = get(key)?.toBooleanStrictOrNull() ?: default

    /** Reads a PEM either inline (…_PEM) or from a file path (…_PATH). */
    private fun pem(inlineKey: String, pathKey: String, default: String = ""): String {
        get(inlineKey)?.takeIf { it.isNotBlank() }?.let { return it.replace("\\n", "\n") }
        get(pathKey)?.takeIf { it.isNotBlank() }?.let { path ->
            val f = File(path)
            if (f.exists()) return f.readText()
        }
        return default
    }

    fun load(): AppConfig {
        val emailProvider = runCatching {
            EmailConfig.Provider.valueOf(get("EMAIL_PROVIDER", "LOG").uppercase())
        }.getOrDefault(EmailConfig.Provider.LOG)

        return AppConfig(
            server = ServerConfig(
                host = get("SERVER_HOST", "0.0.0.0"),
                port = int("SERVER_PORT", 8080),
            ),
            appName = get("APP_NAME", "Identity"),
            frontendBaseUrl = get("FRONTEND_BASE_URL", "http://localhost:3000"),
            corsAllowedHosts = get("CORS_ALLOWED_HOSTS", "*")
                .split(",").map { it.trim() }.filter { it.isNotEmpty() },
            swaggerEnabled = bool("SWAGGER_ENABLED", true),
            adminDashboardEnabled = bool("ADMIN_DASHBOARD_ENABLED", true),
            database = DatabaseConfig(
                jdbcUrl = get("DB_JDBC_URL", "jdbc:postgresql://localhost:5432/identity"),
                username = get("DB_USERNAME", "identity"),
                password = get("DB_PASSWORD", "identity"),
                maxPoolSize = int("DB_MAX_POOL_SIZE", 10),
                minIdle = int("DB_MIN_IDLE", 2),
                runMigrationsOnStart = bool("DB_RUN_MIGRATIONS", true),
            ),
            jwtKeys = JwtKeyConfig(
                privateKeyPem = pem("JWT_PRIVATE_KEY_PEM", "JWT_PRIVATE_KEY_PATH"),
                publicKeyPem = pem("JWT_PUBLIC_KEY_PEM", "JWT_PUBLIC_KEY_PATH"),
                keyId = get("JWT_KEY_ID", "identity-key-1"),
            ),
            email = EmailConfig(
                provider = emailProvider,
                fromAddress = get("EMAIL_FROM_ADDRESS", "no-reply@identity.local"),
                fromName = get("EMAIL_FROM_NAME", "Identity"),
                resendApiKey = get("RESEND_API_KEY"),
                smtpHost = get("SMTP_HOST"),
                smtpPort = int("SMTP_PORT", 587),
                smtpUsername = get("SMTP_USERNAME"),
                smtpPassword = get("SMTP_PASSWORD"),
            ),
            oauth = OAuthConfig(
                google = providerConfig("GOOGLE"),
                github = providerConfig("GITHUB"),
            ),
            rateLimit = RateLimitConfig(
                enabled = bool("RATE_LIMIT_ENABLED", false),
                limit = int("RATE_LIMIT_MAX", 10),
                windowSeconds = long("RATE_LIMIT_WINDOW_SECONDS", 60),
            ),
            policy = AuthPolicy(
                accessTokenTtl = long("ACCESS_TOKEN_TTL_MINUTES", 15).minutes,
                refreshTokenTtl = long("REFRESH_TOKEN_TTL_DAYS", 7).days,
                serviceTokenTtl = long("SERVICE_TOKEN_TTL_MINUTES", 15).minutes,
                emailVerificationTtl = long("EMAIL_VERIFICATION_TTL_HOURS", 24).hours,
                passwordResetTtl = long("PASSWORD_RESET_TTL_HOURS", 1).hours,
                issuer = get("JWT_ISSUER", "identity"),
                defaultAudience = get("JWT_DEFAULT_AUDIENCE", "identity"),
            ),
        )
    }

    private fun providerConfig(prefix: String): OAuthProviderConfig? {
        val id = get("OAUTH_${prefix}_CLIENT_ID")
        val secret = get("OAUTH_${prefix}_CLIENT_SECRET")
        return if (!id.isNullOrBlank() && !secret.isNullOrBlank()) OAuthProviderConfig(id, secret) else null
    }
}

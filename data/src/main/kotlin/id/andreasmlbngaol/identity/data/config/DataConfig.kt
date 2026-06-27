package id.andreasmlbngaol.identity.data.config

/** Database connection settings consumed by the data layer. */
data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int = 10,
    val minIdle: Int = 2,
    val runMigrationsOnStart: Boolean = true,
)

/** RSA key material (PEM) used to sign/verify access tokens. */
data class JwtKeyConfig(
    val privateKeyPem: String,
    val publicKeyPem: String,
    /** Stable key id published in the JWKS document. */
    val keyId: String,
)

/** Selects and configures the active email transport. */
data class EmailConfig(
    val provider: Provider = Provider.LOG,
    val fromAddress: String = "no-reply@identity.local",
    val fromName: String = "Identity",
    // Resend
    val resendApiKey: String? = null,
    // SMTP
    val smtpHost: String? = null,
    val smtpPort: Int = 587,
    val smtpUsername: String? = null,
    val smtpPassword: String? = null,
) {
    enum class Provider { LOG, RESEND, SMTP }
}

/** Per-provider OAuth client credentials. */
data class OAuthProviderConfig(
    val clientId: String,
    val clientSecret: String,
)

data class OAuthConfig(
    val google: OAuthProviderConfig? = null,
    val github: OAuthProviderConfig? = null,
)

data class RateLimitConfig(
    val enabled: Boolean = false,
    val limit: Int = 10,
    val windowSeconds: Long = 60,
)

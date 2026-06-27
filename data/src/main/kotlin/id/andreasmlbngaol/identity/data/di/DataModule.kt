package id.andreasmlbngaol.identity.data.di

import id.andreasmlbngaol.identity.data.audit.AsyncAuditLogger
import id.andreasmlbngaol.identity.data.config.EmailConfig
import id.andreasmlbngaol.identity.data.config.OAuthConfig
import id.andreasmlbngaol.identity.data.db.DatabaseFactory
import id.andreasmlbngaol.identity.data.db.ExposedTransactionRunner
import id.andreasmlbngaol.identity.data.email.LogEmailSender
import id.andreasmlbngaol.identity.data.email.ResendEmailSender
import id.andreasmlbngaol.identity.data.email.SmtpEmailSender
import id.andreasmlbngaol.identity.data.oauth.GitHubOAuthProvider
import id.andreasmlbngaol.identity.data.oauth.GoogleOAuthProvider
import id.andreasmlbngaol.identity.data.ratelimit.InMemoryRateLimiter
import id.andreasmlbngaol.identity.data.repository.AuditLogRepositoryImpl
import id.andreasmlbngaol.identity.data.repository.ClientRepositoryImpl
import id.andreasmlbngaol.identity.data.repository.EmailVerificationTokenRepositoryImpl
import id.andreasmlbngaol.identity.data.repository.OAuthAccountRepositoryImpl
import id.andreasmlbngaol.identity.data.repository.PasswordResetTokenRepositoryImpl
import id.andreasmlbngaol.identity.data.repository.RefreshTokenRepositoryImpl
import id.andreasmlbngaol.identity.data.repository.RoleRepositoryImpl
import id.andreasmlbngaol.identity.data.repository.UserRepositoryImpl
import id.andreasmlbngaol.identity.data.security.Argon2PasswordHasher
import id.andreasmlbngaol.identity.data.security.JwtTokenIssuer
import id.andreasmlbngaol.identity.data.security.RsaKeys
import id.andreasmlbngaol.identity.data.security.Sha256SecretHasher
import id.andreasmlbngaol.identity.data.support.SystemClock
import id.andreasmlbngaol.identity.data.support.UuidGenerator
import id.andreasmlbngaol.identity.domain.enums.AuthProvider
import id.andreasmlbngaol.identity.domain.repository.AuditLogRepository
import id.andreasmlbngaol.identity.domain.repository.ClientRepository
import id.andreasmlbngaol.identity.domain.repository.EmailVerificationTokenRepository
import id.andreasmlbngaol.identity.domain.repository.OAuthAccountRepository
import id.andreasmlbngaol.identity.domain.repository.PasswordResetTokenRepository
import id.andreasmlbngaol.identity.domain.repository.RefreshTokenRepository
import id.andreasmlbngaol.identity.domain.repository.RoleRepository
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.EmailSender
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import id.andreasmlbngaol.identity.domain.service.OAuthProvider
import id.andreasmlbngaol.identity.domain.service.PasswordHasher
import id.andreasmlbngaol.identity.domain.service.RateLimiter
import id.andreasmlbngaol.identity.domain.service.SecretHasher
import id.andreasmlbngaol.identity.domain.service.TokenIssuer
import id.andreasmlbngaol.identity.domain.service.TransactionRunner
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for the data layer. It binds every domain port (repository /
 * service interface) to its concrete, technology-specific implementation, so
 * the rest of the app depends only on domain abstractions.
 *
 * The [DatabaseFactory] and configuration objects are expected to be provided
 * by the app layer (so the data module stays free of process bootstrapping).
 */
fun dataModule(): Module = module {
    // --- Infrastructure singletons ---------------------------------------
    // DatabaseFactory.connect() registers the default Exposed database, so
    // repositories can use suspendTransaction { } without an explicit handle.
    single { get<DatabaseFactory>().database }
    single<TransactionRunner> { ExposedTransactionRunner() }

    single<Clock> { SystemClock() }
    single<IdGenerator> { UuidGenerator() }
    single<PasswordHasher> { Argon2PasswordHasher() }
    single<SecretHasher> { Sha256SecretHasher() }

    single { RsaKeys(get()) }
    single<TokenIssuer> { JwtTokenIssuer(get(), get(), get()) }
    single { get<TokenIssuer>() as JwtTokenIssuer } // expose JWKS helper

    single<RateLimiter> { InMemoryRateLimiter(get(), get()) }

    // --- Shared HTTP client for OAuth providers --------------------------
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    // --- Email transport (selected by config) ----------------------------
    single<EmailSender> {
        val config = get<EmailConfig>()
        when (config.provider) {
            EmailConfig.Provider.RESEND -> ResendEmailSender(config)
            EmailConfig.Provider.SMTP -> SmtpEmailSender(config)
            EmailConfig.Provider.LOG -> LogEmailSender()
        }
    }

    // --- OAuth providers -------------------------------------------------
    single<Map<AuthProvider, OAuthProvider>> {
        val config = get<OAuthConfig>()
        val client = get<HttpClient>()
        buildMap {
            config.google?.let { put(AuthProvider.GOOGLE, GoogleOAuthProvider(it, client)) }
            config.github?.let { put(AuthProvider.GITHUB, GitHubOAuthProvider(it, client)) }
        }
    }

    // --- Repositories ----------------------------------------------------
    single<UserRepository> { UserRepositoryImpl() }
    single<RoleRepository> { RoleRepositoryImpl() }
    single<RefreshTokenRepository> { RefreshTokenRepositoryImpl(get()) }
    single<OAuthAccountRepository> { OAuthAccountRepositoryImpl() }
    single<ClientRepository> { ClientRepositoryImpl() }
    single<EmailVerificationTokenRepository> { EmailVerificationTokenRepositoryImpl(get()) }
    single<PasswordResetTokenRepository> { PasswordResetTokenRepositoryImpl(get()) }
    single<AuditLogRepository> { AuditLogRepositoryImpl() }

    // --- Audit logger (async, depends on its repository) -----------------
    single<AuditLogger> { AsyncAuditLogger(get(), get(), get()) }
}

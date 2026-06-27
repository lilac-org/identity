package id.andreasmlbngaol.identity.di

import id.andreasmlbngaol.identity.config.AppConfig
import id.andreasmlbngaol.identity.data.config.EmailConfig
import id.andreasmlbngaol.identity.data.config.OAuthConfig
import id.andreasmlbngaol.identity.data.config.RateLimitConfig
import id.andreasmlbngaol.identity.data.config.JwtKeyConfig
import id.andreasmlbngaol.identity.data.config.DatabaseConfig
import id.andreasmlbngaol.identity.data.db.DatabaseFactory
import id.andreasmlbngaol.identity.data.security.JwtTokenIssuer
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.service.EmailTemplates
import id.andreasmlbngaol.identity.domain.usecase.account.ChangePasswordUseCase
import id.andreasmlbngaol.identity.domain.usecase.account.ForgotPasswordUseCase
import id.andreasmlbngaol.identity.domain.usecase.account.GetCurrentUserUseCase
import id.andreasmlbngaol.identity.domain.usecase.account.ResendVerificationUseCase
import id.andreasmlbngaol.identity.domain.usecase.account.ResetPasswordUseCase
import id.andreasmlbngaol.identity.domain.usecase.account.UpdateProfileUseCase
import id.andreasmlbngaol.identity.domain.usecase.account.VerifyEmailUseCase
import id.andreasmlbngaol.identity.domain.usecase.admin.GetUserUseCase
import id.andreasmlbngaol.identity.domain.usecase.admin.ListAuditLogsUseCase
import id.andreasmlbngaol.identity.domain.usecase.admin.ListRolesUseCase
import id.andreasmlbngaol.identity.domain.usecase.admin.ListUsersUseCase
import id.andreasmlbngaol.identity.domain.usecase.admin.ManageUserRolesUseCase
import id.andreasmlbngaol.identity.domain.usecase.admin.SetUserStatusUseCase
import id.andreasmlbngaol.identity.domain.usecase.auth.LoginUseCase
import id.andreasmlbngaol.identity.domain.usecase.auth.LogoutUseCase
import id.andreasmlbngaol.identity.domain.usecase.auth.RefreshTokenUseCase
import id.andreasmlbngaol.identity.domain.usecase.auth.RegisterUseCase
import id.andreasmlbngaol.identity.domain.usecase.client.ClientCredentialsUseCase
import id.andreasmlbngaol.identity.domain.usecase.oauth.OAuthLoginUseCase
import id.andreasmlbngaol.identity.presentation.config.FrontendLinks
import id.andreasmlbngaol.identity.presentation.config.HttpRuntimeConfig
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * App-layer wiring. This is the only module that depends on every other layer:
 * it provides configuration objects + the [DatabaseFactory] consumed by the
 * data module, and constructs the domain use cases (whose dependencies are the
 * domain ports the data module binds). The presentation layer receives a single
 * [ApiDependencies] aggregate.
 */
fun appModule(config: AppConfig): Module = module {
    // --- Configuration objects consumed by the data module ---------------
    single { config }
    single<DatabaseConfig> { config.database }
    single<JwtKeyConfig> { config.jwtKeys }
    single<EmailConfig> { config.email }
    single<OAuthConfig> { config.oauth }
    single<RateLimitConfig> { config.rateLimit }
    single<AuthPolicy> { config.policy }

    single { DatabaseFactory(get()) }

    // --- Presentation-facing configuration -------------------------------
    single { EmailTemplates(config.appName) }
    single { FrontendLinks(baseUrl = config.frontendBaseUrl) }
    single {
        HttpRuntimeConfig(
            allowedHosts = config.corsAllowedHosts,
            swaggerEnabled = config.swaggerEnabled,
            adminDashboardEnabled = config.adminDashboardEnabled,
            behindProxy = config.behindProxy,
            cookieSecure = config.cookieSecure,
        )
    }
}

/**
 * Constructs the domain use cases from the ports provided by the data module,
 * then bundles everything the HTTP layer needs into [ApiDependencies].
 */
fun useCaseModule(): Module = module {
    // --- Authentication --------------------------------------------------
    single {
        val links = get<FrontendLinks>()
        RegisterUseCase(
            users = get(), roles = get(), verificationTokens = get(),
            passwordHasher = get(), secretHasher = get(), tokenIssuer = get(),
            emailSender = get(), emailTemplates = get(), audit = get(),
            idGenerator = get(), clock = get(), transaction = get(), policy = get(),
            verificationUrlBuilder = { token -> links.verifyEmailUrl(token) },
        )
    }
    single {
        LoginUseCase(
            users = get(), refreshTokens = get(), passwordHasher = get(),
            secretHasher = get(), tokenIssuer = get(), audit = get(),
            idGenerator = get(), clock = get(), policy = get(),
        )
    }
    single {
        RefreshTokenUseCase(
            users = get(), refreshTokens = get(), secretHasher = get(),
            tokenIssuer = get(), audit = get(), idGenerator = get(),
            clock = get(), transaction = get(), policy = get(),
        )
    }
    single { LogoutUseCase(refreshTokens = get(), secretHasher = get(), audit = get()) }

    // --- Account / profile ----------------------------------------------
    single { VerifyEmailUseCase(users = get(), tokens = get(), secretHasher = get(), audit = get(), clock = get()) }
    single {
        val links = get<FrontendLinks>()
        ResendVerificationUseCase(
            users = get(), tokens = get(), secretHasher = get(), emailSender = get(),
            emailTemplates = get(), idGenerator = get(), clock = get(), policy = get(),
            verificationUrlBuilder = { token -> links.verifyEmailUrl(token) },
        )
    }
    single {
        val links = get<FrontendLinks>()
        ForgotPasswordUseCase(
            users = get(), tokens = get(), secretHasher = get(), emailSender = get(),
            emailTemplates = get(), audit = get(), idGenerator = get(), clock = get(),
            policy = get(), resetUrlBuilder = { token -> links.resetPasswordUrl(token) },
        )
    }
    single {
        ResetPasswordUseCase(
            users = get(), tokens = get(), refreshTokens = get(), passwordHasher = get(),
            secretHasher = get(), audit = get(), clock = get(), policy = get(),
        )
    }
    single {
        ChangePasswordUseCase(
            users = get(), refreshTokens = get(), passwordHasher = get(),
            audit = get(), clock = get(), policy = get(),
        )
    }
    single { GetCurrentUserUseCase(users = get()) }
    single { UpdateProfileUseCase(users = get(), audit = get(), clock = get()) }

    // --- Clients / OAuth -------------------------------------------------
    single { ClientCredentialsUseCase(clients = get(), passwordHasher = get(), tokenIssuer = get(), clock = get()) }
    single {
        OAuthLoginUseCase(
            providers = get(), users = get(), oauthAccounts = get(), roles = get(),
            audit = get(), idGenerator = get(), clock = get(), transaction = get(),
            loginUseCase = get(),
        )
    }

    // --- Admin -----------------------------------------------------------
    single { ListUsersUseCase(users = get()) }
    single { GetUserUseCase(users = get()) }
    single { SetUserStatusUseCase(users = get(), refreshTokens = get(), audit = get(), clock = get()) }
    single { ManageUserRolesUseCase(users = get(), roles = get(), refreshTokens = get(), audit = get()) }
    single { ListRolesUseCase(roles = get()) }
    single { ListAuditLogsUseCase(auditLogs = get()) }

    // --- Aggregate handed to the HTTP layer ------------------------------
    single {
        val issuer = get<JwtTokenIssuer>()
        ApiDependencies(
            register = get(), login = get(), logout = get(), refresh = get(),
            verifyEmail = get(), resendVerification = get(), forgotPassword = get(),
            resetPassword = get(), changePassword = get(), getCurrentUser = get(),
            updateProfile = get(), clientCredentials = get(), oauthLogin = get(),
            listUsers = get(), getUser = get(), setUserStatus = get(),
            manageRoles = get(), listRoles = get(), listAuditLogs = get(),
            rateLimiter = get(), policy = get(),
            jwksProvider = { issuer.jwks() },
            frontendLinks = get(), httpConfig = get(),
        )
    }
}

package id.andreasmlbngaol.identity.presentation.di

import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.service.RateLimiter
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

/**
 * A single aggregate of everything the HTTP routes need. Resolved once from the
 * DI container at startup and handed to the route builders, keeping individual
 * handlers free of container lookups.
 */
class ApiDependencies(
    val register: RegisterUseCase,
    val login: LoginUseCase,
    val logout: LogoutUseCase,
    val refresh: RefreshTokenUseCase,
    val verifyEmail: VerifyEmailUseCase,
    val resendVerification: ResendVerificationUseCase,
    val forgotPassword: ForgotPasswordUseCase,
    val resetPassword: ResetPasswordUseCase,
    val changePassword: ChangePasswordUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val updateProfile: UpdateProfileUseCase,
    val clientCredentials: ClientCredentialsUseCase,
    val oauthLogin: OAuthLoginUseCase,
    val listUsers: ListUsersUseCase,
    val getUser: GetUserUseCase,
    val setUserStatus: SetUserStatusUseCase,
    val manageRoles: ManageUserRolesUseCase,
    val listRoles: ListRolesUseCase,
    val listAuditLogs: ListAuditLogsUseCase,
    val rateLimiter: RateLimiter,
    val policy: AuthPolicy,
    /** Supplies the JWKS document (public keys) for token verification. */
    val jwksProvider: () -> Map<String, Any>,
    val frontendLinks: FrontendLinks,
    val httpConfig: HttpRuntimeConfig,
)

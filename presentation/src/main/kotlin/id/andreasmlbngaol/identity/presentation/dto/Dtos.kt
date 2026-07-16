package id.andreasmlbngaol.identity.presentation.dto

import kotlinx.serialization.Serializable

// ---- Requests -----------------------------------------------------------

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val fullName: String? = null,
    val phoneNumber: String? = null,
)

@Serializable
data class LoginRequest(
    /** Email, username, or phone number. */
    val identifier: String,
    val password: String,
    /** Optional target audience(s) for the issued access token. */
    val audience: List<String> = emptyList(),
    val clientId: String? = null,
    val useCookie: Boolean = false,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String? = null,
    val clientId: String? = null,
    val audience: List<String> = emptyList(),
)

@Serializable
data class LogoutRequest(
    val refreshToken: String? = null,
    val clientId: String? = null,
    val allDevices: Boolean = false,
)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val token: String, val newPassword: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class ResendVerificationRequest(val email: String)

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
    val clearPhone: Boolean = false,
)

@Serializable
data class ClientCredentialsRequest(
    val clientId: String,
    val clientSecret: String,
    val scopes: List<String> = emptyList(),
    val audience: List<String> = emptyList(),
)

@Serializable
data class AuthorizationCodeExchangeRequest(
    val clientId: String,
    val code: String,
    val redirectUri: String,
    val codeVerifier: String,
)

@Serializable
data class AssignRoleRequest(val role: String)

// ---- Responses ----------------------------------------------------------

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val accessTokenExpiresAt: String,
    val refreshTokenExpiresAt: String? = null,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val status: String,
    val emailVerified: Boolean,
    val fullName: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val phoneVerified: Boolean = false,
    val dateOfBirth: String? = null,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class RoleResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val isSystem: Boolean,
    val permissions: List<String> = emptyList(),
)

@Serializable
data class AuditLogResponse(
    val id: String,
    val action: String,
    val userId: String? = null,
    val clientId: String? = null,
    val ipAddress: String? = null,
    val success: Boolean,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: String,
)

@Serializable
data class RegisterResponse(
    val user: UserResponse,
    val message: String = "Registration successful. Please verify your email.",
)

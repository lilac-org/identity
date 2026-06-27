package id.andreasmlbngaol.identity.domain.service

import id.andreasmlbngaol.identity.domain.enums.AuthProvider

/**
 * Exchanges an OAuth authorization code for the external user's profile.
 * One implementation per provider (Google, GitHub) lives in the data layer.
 */
interface OAuthProvider {
    val provider: AuthProvider

    /** Builds the provider authorization URL the user is redirected to. */
    fun authorizationUrl(state: String, redirectUri: String): String

    /** Exchanges [code] for the verified external profile. */
    suspend fun exchangeCode(code: String, redirectUri: String): OAuthUserProfile
}

data class OAuthUserProfile(
    val provider: AuthProvider,
    val providerUserId: String,
    val email: String?,
    val emailVerified: Boolean,
    val fullName: String?,
    val photoUrl: String?,
)

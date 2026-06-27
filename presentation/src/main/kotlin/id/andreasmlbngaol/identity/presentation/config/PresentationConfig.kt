package id.andreasmlbngaol.identity.presentation.config

/**
 * Links used to build the user-facing URLs embedded in emails. Supplied by the
 * app layer so the presentation layer does not need to know how configuration
 * is loaded.
 */
data class FrontendLinks(
    val baseUrl: String,
    val verifyEmailPath: String = "/verify-email",
    val resetPasswordPath: String = "/reset-password",
    val oauthRedirectPath: String = "/oauth/callback",
) {
    fun verifyEmailUrl(token: String): String = "$baseUrl$verifyEmailPath?token=$token"
    fun resetPasswordUrl(token: String): String = "$baseUrl$resetPasswordPath?token=$token"
}

/** HTTP-facing settings consumed by presentation plugins. */
data class HttpRuntimeConfig(
    val allowedHosts: List<String> = listOf("*"),
    val swaggerEnabled: Boolean = true,
    val adminDashboardEnabled: Boolean = true,
)

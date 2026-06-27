package id.andreasmlbngaol.identity.domain.policy

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tunable, transport-agnostic security policy. Concrete values are supplied by
 * configuration in the app layer; defaults follow the decisions agreed during
 * design (15-minute access tokens, 7-day refresh tokens with rotation).
 */
data class AuthPolicy(
    val accessTokenTtl: Duration = 15.minutes,
    val refreshTokenTtl: Duration = 7.days,
    val serviceTokenTtl: Duration = 15.minutes,
    val emailVerificationTtl: Duration = 24.hours,
    val passwordResetTtl: Duration = 1.hours,
    val issuer: String = "identity",
    /** Default audience embedded when a caller does not request specific ones. */
    val defaultAudience: String = "identity",
    val minPasswordLength: Int = 8,
    val maxPasswordLength: Int = 128,
)

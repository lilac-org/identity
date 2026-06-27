package id.andreasmlbngaol.identity.domain.service

/**
 * Abstraction for throttling sensitive operations (login, password reset...).
 *
 * NOTE: A working in-memory implementation is provided in the data layer, but
 * it is intentionally NOT enforced yet (see configuration `rateLimit.enabled`)
 * because the project is still in early development. Flip it on for production.
 */
interface RateLimiter {
    /**
     * @return true if the action is allowed, false if the caller is over the
     * limit for the given [key] within the configured window.
     */
    suspend fun tryAcquire(key: String): Boolean
}

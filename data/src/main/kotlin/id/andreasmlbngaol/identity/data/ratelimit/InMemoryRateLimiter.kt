package id.andreasmlbngaol.identity.data.ratelimit

import id.andreasmlbngaol.identity.data.config.RateLimitConfig
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.RateLimiter
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple fixed-window, in-memory rate limiter. It is fully functional but, per
 * the agreed plan, NOT enforced during early development (see
 * `rateLimit.enabled`). For multi-instance production, swap this for a shared
 * Redis-backed implementation behind the same [RateLimiter] interface.
 */
class InMemoryRateLimiter(
    private val config: RateLimitConfig,
    private val clock: Clock,
) : RateLimiter {
    private data class Window(var windowStartMs: Long, var count: Int)

    private val buckets = ConcurrentHashMap<String, Window>()

    override suspend fun tryAcquire(key: String): Boolean {
        if (!config.enabled) return true
        val nowMs = clock.now().toEpochMilliseconds()
        val windowMs = config.windowSeconds * 1000
        val window = buckets.compute(key) { _, existing ->
            if (existing == null || nowMs - existing.windowStartMs >= windowMs) {
                Window(nowMs, 1)
            } else {
                existing.count += 1
                existing
            }
        }!!
        return window.count <= config.limit
    }
}

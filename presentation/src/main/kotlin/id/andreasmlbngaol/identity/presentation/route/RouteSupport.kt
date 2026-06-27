package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.error.RateLimitedException
import id.andreasmlbngaol.identity.domain.model.PageRequest
import id.andreasmlbngaol.identity.domain.service.RateLimiter
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin

/**
 * Reads 1-based `page`, `size`, and `search` query params into a domain
 * [PageRequest] (which is 0-based internally).
 */
fun ApplicationCall.pageRequest(): PageRequest {
    val page = ((request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)) - 1
    val size = request.queryParameters["size"]?.toIntOrNull()?.coerceIn(1, PageRequest.MAX_PAGE_SIZE) ?: 20
    val search = request.queryParameters["search"]?.takeIf { it.isNotBlank() }
    return PageRequest(page = page, size = size, search = search)
}

/**
 * Applies the configured rate limiter keyed by client IP + route. Implemented
 * now but a no-op while disabled, per the agreed rollout plan.
 */
suspend fun RateLimiter.enforce(call: ApplicationCall, bucket: String) {
    val key = "$bucket:${call.request.origin.remoteHost}"
    if (!tryAcquire(key)) {
        throw RateLimitedException("Too many requests, please try again later")
    }
}

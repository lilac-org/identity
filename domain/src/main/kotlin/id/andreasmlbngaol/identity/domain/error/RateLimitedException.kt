package id.andreasmlbngaol.identity.domain.error

/** Raised when a caller exceeds the configured request rate for an endpoint. */
class RateLimitedException(
    message: String = "Too many requests",
) : DomainException(ErrorCode.RATE_LIMITED, message)

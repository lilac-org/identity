package id.andreasmlbngaol.identity.domain.error

/**
 * Stable, transport-agnostic error codes. The presentation layer maps these to
 * HTTP status codes; the domain never knows about HTTP.
 */
enum class ErrorCode {
    VALIDATION_FAILED,
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_USED,
    USERNAME_ALREADY_USED,
    PHONE_ALREADY_USED,
    USER_NOT_FOUND,
    ACCOUNT_NOT_ACTIVE,
    EMAIL_NOT_VERIFIED,
    EMAIL_ALREADY_VERIFIED,
    TOKEN_INVALID,
    TOKEN_EXPIRED,
    TOKEN_REUSE_DETECTED,
    PERMISSION_DENIED,
    CLIENT_INVALID,
    OAUTH_FAILED,
    CONFLICT,
    RATE_LIMITED,
    INTERNAL,
}

/**
 * Base type for all expected, business-rule failures. Carries a machine-readable
 * [code] plus an optional map of field-level validation errors.
 */
sealed class DomainException(
    val code: ErrorCode,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class ValidationException(
    message: String = "Validation failed",
    fieldErrors: Map<String, String> = emptyMap(),
) : DomainException(ErrorCode.VALIDATION_FAILED, message, fieldErrors)

class InvalidCredentialsException(
    message: String = "Invalid credentials",
) : DomainException(ErrorCode.INVALID_CREDENTIALS, message)

class ConflictException(
    code: ErrorCode,
    message: String,
) : DomainException(code, message)

class NotFoundException(
    message: String = "Resource not found",
) : DomainException(ErrorCode.USER_NOT_FOUND, message)

class AccountNotActiveException(
    message: String = "Account is not active",
) : DomainException(ErrorCode.ACCOUNT_NOT_ACTIVE, message)

class EmailNotVerifiedException(
    message: String = "Email address is not verified",
) : DomainException(ErrorCode.EMAIL_NOT_VERIFIED, message)

class TokenException(
    code: ErrorCode,
    message: String,
) : DomainException(code, message)

class PermissionDeniedException(
    message: String = "You do not have permission to perform this action",
) : DomainException(ErrorCode.PERMISSION_DENIED, message)

class ClientException(
    message: String = "Invalid client",
) : DomainException(ErrorCode.CLIENT_INVALID, message)

class OAuthException(
    message: String = "OAuth authentication failed",
    cause: Throwable? = null,
) : DomainException(ErrorCode.OAUTH_FAILED, message, cause = cause)

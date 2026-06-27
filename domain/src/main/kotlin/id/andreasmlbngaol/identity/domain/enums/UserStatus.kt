package id.andreasmlbngaol.identity.domain.enums

/**
 * Lifecycle state of a user account.
 */
enum class UserStatus {
    /** Registered but email not yet verified. */
    PENDING_VERIFICATION,
    ACTIVE,
    /** Temporarily blocked by an administrator. */
    SUSPENDED,
    /** Soft-deleted. Retained for auditing but cannot authenticate. */
    DELETED,
}

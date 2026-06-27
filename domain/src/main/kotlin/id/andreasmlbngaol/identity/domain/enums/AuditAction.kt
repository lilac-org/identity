package id.andreasmlbngaol.identity.domain.enums

/**
 * Canonical list of auditable security events. Stored with every audit log
 * entry so events can be filtered and analysed consistently.
 */
enum class AuditAction {
    USER_REGISTERED,
    USER_LOGIN_SUCCEEDED,
    USER_LOGIN_FAILED,
    USER_LOGGED_OUT,
    TOKEN_REFRESHED,
    REFRESH_TOKEN_REUSE_DETECTED,
    EMAIL_VERIFICATION_REQUESTED,
    EMAIL_VERIFIED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    PASSWORD_CHANGED,
    PROFILE_UPDATED,
    OAUTH_ACCOUNT_LINKED,
    OAUTH_LOGIN,
    USER_SUSPENDED,
    USER_REACTIVATED,
    USER_SOFT_DELETED,
    ROLE_ASSIGNED,
    ROLE_REVOKED,
    ADMIN_ACTION,
}

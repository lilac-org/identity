package id.andreasmlbngaol.identity.domain.enums

/**
 * Distinguishes the audience/intent of an issued JWT.
 */
enum class TokenType {
    ACCESS,
    REFRESH,
    /** Machine-to-machine token issued through the client-credentials grant. */
    SERVICE,
}

package id.andreasmlbngaol.identity.domain.enums

/**
 * The kind of identifier a user supplied when logging in. A user may sign in
 * with whichever single identifier they prefer.
 */
enum class LoginIdentifierType {
    EMAIL,
    USERNAME,
    PHONE,
}

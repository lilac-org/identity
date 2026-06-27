package id.andreasmlbngaol.identity.domain.service

/**
 * Abstraction over password hashing so the algorithm (Argon2id by default) can
 * be swapped without touching business logic.
 */
interface PasswordHasher {
    fun hash(rawPassword: CharArray): String
    fun verify(rawPassword: CharArray, hash: String): Boolean

    fun hash(rawPassword: String): String = hash(rawPassword.toCharArray())
    fun verify(rawPassword: String, hash: String): Boolean =
        verify(rawPassword.toCharArray(), hash)
}

package id.andreasmlbngaol.identity.data.security

import de.mkammerer.argon2.Argon2Factory
import id.andreasmlbngaol.identity.domain.service.PasswordHasher

/**
 * Argon2id password hasher (the OWASP-recommended default). Parameters follow
 * sensible server-side defaults and can be tuned per deployment. The raw
 * password char array is wiped after use to limit how long the secret lingers
 * in memory.
 */
class Argon2PasswordHasher(
    private val iterations: Int = 3,
    private val memoryKib: Int = 65_536,
    private val parallelism: Int = 1,
) : PasswordHasher {

    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    override fun hash(rawPassword: CharArray): String =
        try {
            argon2.hash(iterations, memoryKib, parallelism, rawPassword)
        } finally {
            argon2.wipeArray(rawPassword)
        }

    override fun verify(rawPassword: CharArray, hash: String): Boolean =
        try {
            argon2.verify(hash, rawPassword)
        } finally {
            argon2.wipeArray(rawPassword)
        }
}

package id.andreasmlbngaol.identity.domain.service

/**
 * One-way hashing for high-entropy opaque secrets (refresh tokens, email/reset
 * tokens). Implemented with SHA-256 because the inputs are random and long,
 * unlike user passwords which require a slow KDF.
 */
interface SecretHasher {
    fun hash(rawSecret: String): String
}

package id.andreasmlbngaol.identity.data.security

import id.andreasmlbngaol.identity.domain.service.SecretHasher
import java.security.MessageDigest

/**
 * SHA-256 hasher for high-entropy opaque secrets (refresh / verification /
 * reset tokens). A fast digest is appropriate here because the inputs are long
 * and random — unlike user passwords, which require a slow KDF. Produces a
 * 64-character lowercase hex string, matching the `varchar(64)` token columns.
 */
class Sha256SecretHasher : SecretHasher {
    override fun hash(rawSecret: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(rawSecret.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

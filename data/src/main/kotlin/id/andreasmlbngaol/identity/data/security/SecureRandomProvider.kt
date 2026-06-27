package id.andreasmlbngaol.identity.data.security

import java.security.SecureRandom

/**
 * A shared, thread-safe [SecureRandom] instance. Creating SecureRandom can be
 * comparatively expensive (seeding), so a single instance is reused for all
 * token generation.
 */
object SecureRandomProvider {
    val instance: SecureRandom = SecureRandom()
}

package id.andreasmlbngaol.identity.domain.service

import kotlin.time.Instant

/** Injectable clock so time-dependent logic is deterministically testable. */
interface Clock {
    fun now(): Instant
}

package id.andreasmlbngaol.identity.data.support

import id.andreasmlbngaol.identity.domain.service.Clock
import kotlin.time.Clock as TimeClock
import kotlin.time.Instant

/** Production [Clock] backed by the system wall clock (UTC instants). */
class SystemClock : Clock {
    override fun now(): Instant = TimeClock.System.now()
}

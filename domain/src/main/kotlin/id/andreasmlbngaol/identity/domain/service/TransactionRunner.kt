package id.andreasmlbngaol.identity.domain.service

/**
 * Allows use cases to group several repository operations into one atomic unit
 * of work without leaking the persistence technology. The data layer provides
 * an Exposed-backed implementation.
 */
interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

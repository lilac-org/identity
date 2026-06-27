package id.andreasmlbngaol.identity.domain.service

import kotlin.uuid.Uuid

/** Injectable id generation, keeping `Uuid` creation out of business logic. */
interface IdGenerator {
    fun newId(): Uuid
}

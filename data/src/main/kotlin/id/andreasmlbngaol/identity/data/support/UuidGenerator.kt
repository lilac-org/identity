package id.andreasmlbngaol.identity.data.support

import id.andreasmlbngaol.identity.domain.service.IdGenerator
import kotlin.uuid.Uuid

/** Production [IdGenerator] producing random (v4) UUIDs. */
class UuidGenerator : IdGenerator {
    override fun newId(): Uuid = Uuid.random()
}

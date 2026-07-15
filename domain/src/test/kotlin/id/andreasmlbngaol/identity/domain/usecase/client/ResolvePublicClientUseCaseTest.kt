package id.andreasmlbngaol.identity.domain.usecase.client

import id.andreasmlbngaol.identity.domain.error.ClientException
import id.andreasmlbngaol.identity.domain.model.Client
import id.andreasmlbngaol.identity.domain.repository.ClientRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ResolvePublicClientUseCaseTest {
    private val clients = mockk<ClientRepository>()
    private val useCase = ResolvePublicClientUseCase(clients)
    private val client = Client(
        id = Uuid.parse("00000000-0000-0000-0000-000000000001"),
        clientId = "afinance",
        name = "AFinance",
        allowedAudiences = setOf("afinance"),
        redirectUris = setOf("https://a-finance.pages.dev/oauth/callback"),
        isConfidential = false,
        createdAt = Instant.parse("2026-07-15T16:00:00Z"),
        updatedAt = Instant.parse("2026-07-15T16:00:00Z"),
    )

    @Test
    fun `resolves enabled public client`() = runTest {
        coEvery { clients.findByClientId("afinance") } returns client

        val result = useCase.execute("afinance", setOf("afinance"))

        assertEquals(client, result)
    }

    @Test
    fun `rejects audience outside client registration`() = runTest {
        coEvery { clients.findByClientId("afinance") } returns client

        assertFailsWith<ClientException> {
            useCase.execute("afinance", setOf("other"))
        }
    }
}

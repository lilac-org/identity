package id.andreasmlbngaol.identity.presentation.route

import id.andreasmlbngaol.identity.domain.model.Client
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TokenCookieSupportTest {
    private val origins = setOf("https://afinance.example.com")

    @Test
    fun `trusted cookie request requires exact origin and header`() {
        assertTrue(
            isTrustedCookieRequest(
                origin = "https://afinance.example.com",
                requestedWith = COOKIE_REQUEST_VALUE,
                allowedOrigins = origins,
            ),
        )
    }

    @Test
    fun `trusted cookie request rejects unknown origin`() {
        assertFalse(
            isTrustedCookieRequest(
                origin = "https://attacker.example.com",
                requestedWith = COOKIE_REQUEST_VALUE,
                allowedOrigins = origins,
            ),
        )
    }

    @Test
    fun `trusted cookie request rejects missing header`() {
        assertFalse(
            isTrustedCookieRequest(
                origin = "https://afinance.example.com",
                requestedWith = null,
                allowedOrigins = origins,
            ),
        )
    }

    @Test
    fun `allowed Web origins come from registered redirect URIs`() {
        val client = Client(
            id = Uuid.parse("00000000-0000-0000-0000-000000000001"),
            clientId = "afinance",
            name = "AFinance",
            allowedAudiences = setOf("afinance"),
            redirectUris = setOf(
                "https://a-finance.pages.dev/oauth/callback",
                "http://localhost:8080/oauth/callback",
            ),
            isConfidential = false,
            createdAt = Instant.parse("2026-07-15T16:00:00Z"),
            updatedAt = Instant.parse("2026-07-15T16:00:00Z"),
        )

        assertTrue("https://a-finance.pages.dev" in client.allowedWebOrigins())
        assertTrue("http://localhost:8080" in client.allowedWebOrigins())
    }
}

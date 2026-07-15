package id.andreasmlbngaol.identity.presentation.mapper

import id.andreasmlbngaol.identity.domain.model.AuthTokens
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.time.Instant

class MappersTest {
    @Test
    fun `cookie token response does not expose refresh token`() {
        val tokens = AuthTokens(
            accessToken = "access",
            accessTokenExpiresAt = Instant.parse("2026-07-15T16:00:00Z"),
            refreshToken = "refresh",
            refreshTokenExpiresAt = Instant.parse("2026-07-22T16:00:00Z"),
        )

        val response = tokens.toResponse(includeRefreshToken = false)

        assertNull(response.refreshToken)
        assertNull(response.refreshTokenExpiresAt)
    }
}

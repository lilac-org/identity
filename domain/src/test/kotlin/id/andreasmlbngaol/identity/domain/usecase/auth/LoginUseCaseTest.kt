package id.andreasmlbngaol.identity.domain.usecase.auth

import id.andreasmlbngaol.identity.domain.enums.UserStatus
import id.andreasmlbngaol.identity.domain.error.AccountNotActiveException
import id.andreasmlbngaol.identity.domain.error.EmailNotVerifiedException
import id.andreasmlbngaol.identity.domain.error.InvalidCredentialsException
import id.andreasmlbngaol.identity.domain.model.RequestContext
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.repository.RefreshTokenRepository
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import id.andreasmlbngaol.identity.domain.service.PasswordHasher
import id.andreasmlbngaol.identity.domain.service.SecretHasher
import id.andreasmlbngaol.identity.domain.service.TokenIssuer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

/**
 * Unit tests for [LoginUseCase]. Collaborators are mocked so the test focuses
 * purely on the authentication decision logic.
 */
class LoginUseCaseTest {

    private val users = mockk<UserRepository>()
    private val refreshTokens = mockk<RefreshTokenRepository>(relaxed = true)
    private val passwordHasher = mockk<PasswordHasher>()
    private val secretHasher = mockk<SecretHasher>()
    private val tokenIssuer = mockk<TokenIssuer>()
    private val audit = mockk<AuditLogger>(relaxed = true)
    private val idGenerator = mockk<IdGenerator>()
    private val clock = mockk<Clock>()

    private val now = Instant.parse("2026-06-27T08:00:00Z")
    private val policy = AuthPolicy(
        accessTokenTtl = 15.minutes,
        refreshTokenTtl = 7.days,
        serviceTokenTtl = 15.minutes,
        emailVerificationTtl = 24.hours,
        passwordResetTtl = 1.hours,
        issuer = "identity",
        defaultAudience = "identity",
    )

    private val useCase = LoginUseCase(
        users, refreshTokens, passwordHasher, secretHasher,
        tokenIssuer, audit, idGenerator, clock, policy,
    )

    private val ctx = RequestContext(ipAddress = "127.0.0.1", userAgent = "junit", clientId = null)

    private fun user(status: UserStatus) = User(
        id = Uuid.random(),
        email = "alice@example.com",
        username = "alice",
        passwordHash = "hash",
        status = status,
        emailVerified = status == UserStatus.ACTIVE,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `successful login issues tokens`() = runTest {
        every { clock.now() } returns now
        coEvery { users.findByAnyIdentifier("alice") } returns user(UserStatus.ACTIVE)
        every { passwordHasher.verify("secret", "hash") } returns true
        every { tokenIssuer.issueAccessToken(any(), any()) } returns
            TokenIssuer.IssuedAccessToken("access.jwt", now.plus(policy.accessTokenTtl))
        every { tokenIssuer.generateRefreshToken() } returns "raw-refresh"
        every { idGenerator.newId() } returns Uuid.random()
        every { secretHasher.hash("raw-refresh") } returns "hashed-refresh"

        val tokens = useCase.execute(LoginUseCase.Command("alice", "secret"), ctx)

        tokens.accessToken shouldBe "access.jwt"
        tokens.refreshToken shouldBe "raw-refresh"
        coVerify(exactly = 1) { refreshTokens.create(any()) }
    }

    @Test
    fun `wrong password is rejected`() = runTest {
        coEvery { users.findByAnyIdentifier("alice") } returns user(UserStatus.ACTIVE)
        every { passwordHasher.verify("wrong", "hash") } returns false

        shouldThrow<InvalidCredentialsException> {
            useCase.execute(LoginUseCase.Command("alice", "wrong"), ctx)
        }
    }

    @Test
    fun `unknown user is rejected without leaking existence`() = runTest {
        coEvery { users.findByAnyIdentifier("ghost") } returns null
        every { passwordHasher.verify(any<String>(), any<String>()) } returns false

        shouldThrow<InvalidCredentialsException> {
            useCase.execute(LoginUseCase.Command("ghost", "secret"), ctx)
        }
    }

    @Test
    fun `suspended account cannot log in`() = runTest {
        coEvery { users.findByAnyIdentifier("alice") } returns user(UserStatus.SUSPENDED)
        every { passwordHasher.verify("secret", "hash") } returns true

        shouldThrow<AccountNotActiveException> {
            useCase.execute(LoginUseCase.Command("alice", "secret"), ctx)
        }
    }

    @Test
    fun `unverified account cannot log in`() = runTest {
        coEvery { users.findByAnyIdentifier("alice") } returns user(UserStatus.PENDING_VERIFICATION)
        every { passwordHasher.verify("secret", "hash") } returns true

        shouldThrow<EmailNotVerifiedException> {
            useCase.execute(LoginUseCase.Command("alice", "secret"), ctx)
        }
    }
}

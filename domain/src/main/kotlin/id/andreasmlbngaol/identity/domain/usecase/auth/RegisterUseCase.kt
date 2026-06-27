package id.andreasmlbngaol.identity.domain.usecase.auth

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.enums.UserStatus
import id.andreasmlbngaol.identity.domain.error.ConflictException
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.domain.model.RequestContext
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.domain.model.VerificationToken
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.repository.EmailVerificationTokenRepository
import id.andreasmlbngaol.identity.domain.repository.RoleRepository
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.EmailSender
import id.andreasmlbngaol.identity.domain.service.EmailTemplates
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import id.andreasmlbngaol.identity.domain.service.PasswordHasher
import id.andreasmlbngaol.identity.domain.service.SecretHasher
import id.andreasmlbngaol.identity.domain.service.TokenIssuer
import id.andreasmlbngaol.identity.domain.service.TransactionRunner
import id.andreasmlbngaol.identity.domain.validation.Validators

class RegisterUseCase(
    private val users: UserRepository,
    private val roles: RoleRepository,
    private val verificationTokens: EmailVerificationTokenRepository,
    private val passwordHasher: PasswordHasher,
    private val secretHasher: SecretHasher,
    private val tokenIssuer: TokenIssuer,
    private val emailSender: EmailSender,
    private val emailTemplates: EmailTemplates,
    private val audit: AuditLogger,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val transaction: TransactionRunner,
    private val policy: AuthPolicy,
    private val verificationUrlBuilder: (rawToken: String) -> String,
) {
    data class Command(
        val email: String,
        val username: String,
        val password: String,
        val fullName: String? = null,
        val phoneNumber: String? = null,
    )

    suspend fun execute(command: Command, ctx: RequestContext): User {
        val email = command.email.trim().lowercase()
        val username = command.username.trim()
        val phone = command.phoneNumber?.trim()?.takeIf { it.isNotBlank() }

        Validators.validateRegistration(
            email = email,
            username = username,
            password = command.password,
            phoneNumber = phone,
            minPasswordLength = policy.minPasswordLength,
            maxPasswordLength = policy.maxPasswordLength,
        )

        if (users.existsByEmail(email)) throw ConflictException(ErrorCode.EMAIL_ALREADY_USED, "Email is already in use")
        if (users.existsByUsername(username)) throw ConflictException(ErrorCode.USERNAME_ALREADY_USED, "Username is already in use")
        if (phone != null && users.existsByPhoneNumber(phone)) {
            throw ConflictException(ErrorCode.PHONE_ALREADY_USED, "Phone number is already in use")
        }

        val now = clock.now()
        val defaultRole = roles.findByName("USER")
        val newUser = User(
            id = idGenerator.newId(),
            email = email,
            username = username,
            passwordHash = passwordHasher.hash(command.password),
            status = UserStatus.PENDING_VERIFICATION,
            emailVerified = false,
            fullName = command.fullName?.trim()?.takeIf { it.isNotBlank() },
            phoneNumber = phone,
            roles = defaultRole?.let { setOf(it) } ?: emptySet(),
            createdAt = now,
            updatedAt = now,
        )

        val rawToken = tokenIssuer.generateRefreshToken()
        val created = transaction.inTransaction {
            val persisted = users.create(newUser)
            defaultRole?.let { roles.assignRoleToUser(persisted.id, it.id) }
            verificationTokens.invalidateAllForUser(persisted.id)
            verificationTokens.create(
                VerificationToken(
                    id = idGenerator.newId(),
                    userId = persisted.id,
                    tokenHash = secretHasher.hash(rawToken),
                    expiresAt = now.plus(policy.emailVerificationTtl),
                    createdAt = now,
                ),
            )
            persisted
        }

        emailSender.send(emailTemplates.verification(email, verificationUrlBuilder(rawToken)))
        audit.record(AuditAction.USER_REGISTERED, created.id, ctx)
        audit.record(AuditAction.EMAIL_VERIFICATION_REQUESTED, created.id, ctx)
        return created
    }
}

package id.andreasmlbngaol.identity.domain.usecase.account

import id.andreasmlbngaol.identity.domain.enums.AuditAction
import id.andreasmlbngaol.identity.domain.enums.UserStatus
import id.andreasmlbngaol.identity.domain.error.ConflictException
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.domain.error.InvalidCredentialsException
import id.andreasmlbngaol.identity.domain.error.NotFoundException
import id.andreasmlbngaol.identity.domain.error.TokenException
import id.andreasmlbngaol.identity.domain.model.RequestContext
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.domain.model.VerificationToken
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.repository.EmailVerificationTokenRepository
import id.andreasmlbngaol.identity.domain.repository.PasswordResetTokenRepository
import id.andreasmlbngaol.identity.domain.repository.RefreshTokenRepository
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.domain.service.AuditLogger
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.EmailSender
import id.andreasmlbngaol.identity.domain.service.EmailTemplates
import id.andreasmlbngaol.identity.domain.service.IdGenerator
import id.andreasmlbngaol.identity.domain.service.PasswordHasher
import id.andreasmlbngaol.identity.domain.service.SecretHasher
import id.andreasmlbngaol.identity.domain.validation.Validators
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class VerifyEmailUseCase(
    private val users: UserRepository,
    private val tokens: EmailVerificationTokenRepository,
    private val secretHasher: SecretHasher,
    private val audit: AuditLogger,
    private val clock: Clock,
) {
    suspend fun execute(rawToken: String, ctx: RequestContext): User {
        val now = clock.now()
        val token = tokens.findByHash(secretHasher.hash(rawToken))
            ?: throw TokenException(ErrorCode.TOKEN_INVALID, "Verification token is invalid")
        if (!token.isUsable(now)) throw TokenException(ErrorCode.TOKEN_EXPIRED, "Verification token expired")

        val user = users.findById(token.userId) ?: throw NotFoundException("User not found")
        val updated = user.copy(
            emailVerified = true,
            status = if (user.status == UserStatus.PENDING_VERIFICATION) UserStatus.ACTIVE else user.status,
            updatedAt = now,
        )
        users.update(updated)
        tokens.markUsed(token.id)
        audit.record(AuditAction.EMAIL_VERIFIED, user.id, ctx)
        return updated
    }
}

class ResendVerificationUseCase(
    private val users: UserRepository,
    private val tokens: EmailVerificationTokenRepository,
    private val secretHasher: SecretHasher,
    private val emailSender: EmailSender,
    private val emailTemplates: EmailTemplates,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val policy: AuthPolicy,
    private val verificationUrlBuilder: (rawToken: String) -> String,
) {
    /** Always succeeds silently to avoid leaking which emails are registered. */
    suspend fun execute(email: String) {
        val user = users.findByEmail(email.trim().lowercase()) ?: return
        if (user.emailVerified) return
        val now = clock.now()
        val raw = java.util.UUID.randomUUID().toString().replace("-", "") +
            java.util.UUID.randomUUID().toString().replace("-", "")
        tokens.invalidateAllForUser(user.id)
        tokens.create(
            VerificationToken(
                id = idGenerator.newId(),
                userId = user.id,
                tokenHash = secretHasher.hash(raw),
                expiresAt = now.plus(policy.emailVerificationTtl),
                createdAt = now,
            ),
        )
        emailSender.send(emailTemplates.verification(user.email, verificationUrlBuilder(raw)))
    }
}

class ForgotPasswordUseCase(
    private val users: UserRepository,
    private val tokens: PasswordResetTokenRepository,
    private val secretHasher: SecretHasher,
    private val emailSender: EmailSender,
    private val emailTemplates: EmailTemplates,
    private val audit: AuditLogger,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val policy: AuthPolicy,
    private val resetUrlBuilder: (rawToken: String) -> String,
) {
    /** Always returns normally; never reveals whether the email exists. */
    suspend fun execute(email: String, ctx: RequestContext) {
        val user = users.findByEmail(email.trim().lowercase()) ?: return
        val now = clock.now()
        val raw = java.util.UUID.randomUUID().toString().replace("-", "") +
            java.util.UUID.randomUUID().toString().replace("-", "")
        tokens.invalidateAllForUser(user.id)
        tokens.create(
            VerificationToken(
                id = idGenerator.newId(),
                userId = user.id,
                tokenHash = secretHasher.hash(raw),
                expiresAt = now.plus(policy.passwordResetTtl),
                createdAt = now,
            ),
        )
        emailSender.send(emailTemplates.passwordReset(user.email, resetUrlBuilder(raw)))
        audit.record(AuditAction.PASSWORD_RESET_REQUESTED, user.id, ctx)
    }
}

class ResetPasswordUseCase(
    private val users: UserRepository,
    private val tokens: PasswordResetTokenRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordHasher: PasswordHasher,
    private val secretHasher: SecretHasher,
    private val audit: AuditLogger,
    private val clock: Clock,
    private val policy: AuthPolicy,
) {
    data class Command(val rawToken: String, val newPassword: String)

    suspend fun execute(command: Command, ctx: RequestContext) {
        Validators.validatePassword(command.newPassword, policy.minPasswordLength, policy.maxPasswordLength)
        val now = clock.now()
        val token = tokens.findByHash(secretHasher.hash(command.rawToken))
            ?: throw TokenException(ErrorCode.TOKEN_INVALID, "Reset token is invalid")
        if (!token.isUsable(now)) throw TokenException(ErrorCode.TOKEN_EXPIRED, "Reset token expired")
        val user = users.findById(token.userId) ?: throw NotFoundException("User not found")

        users.update(user.copy(passwordHash = passwordHasher.hash(command.newPassword), updatedAt = now))
        tokens.markUsed(token.id)
        // Invalidate every session after a password reset.
        users.incrementTokenVersion(user.id)
        refreshTokens.revokeAllForUser(user.id)
        audit.record(AuditAction.PASSWORD_RESET_COMPLETED, user.id, ctx)
    }
}

class ChangePasswordUseCase(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordHasher: PasswordHasher,
    private val audit: AuditLogger,
    private val clock: Clock,
    private val policy: AuthPolicy,
) {
    data class Command(val userId: Uuid, val currentPassword: String, val newPassword: String)

    suspend fun execute(command: Command, ctx: RequestContext) {
        val user = users.findById(command.userId) ?: throw NotFoundException("User not found")
        val hash = user.passwordHash ?: throw InvalidCredentialsException("No password set for this account")
        if (!passwordHasher.verify(command.currentPassword, hash)) throw InvalidCredentialsException()
        Validators.validatePassword(command.newPassword, policy.minPasswordLength, policy.maxPasswordLength)

        val now = clock.now()
        users.update(user.copy(passwordHash = passwordHasher.hash(command.newPassword), updatedAt = now))
        users.incrementTokenVersion(user.id)
        refreshTokens.revokeAllForUser(user.id)
        audit.record(AuditAction.PASSWORD_CHANGED, user.id, ctx)
    }
}

class GetCurrentUserUseCase(private val users: UserRepository) {
    suspend fun execute(userId: Uuid): User =
        users.findById(userId) ?: throw NotFoundException("User not found")
}

class UpdateProfileUseCase(
    private val users: UserRepository,
    private val audit: AuditLogger,
    private val clock: Clock,
) {
    data class Command(
        val userId: Uuid,
        val fullName: String? = null,
        val photoUrl: String? = null,
        val phoneNumber: String? = null,
        val dateOfBirth: LocalDate? = null,
        val clearPhone: Boolean = false,
    )

    suspend fun execute(command: Command, ctx: RequestContext): User {
        val user = users.findById(command.userId) ?: throw NotFoundException("User not found")
        val newPhone = when {
            command.clearPhone -> null
            command.phoneNumber != null -> {
                val p = command.phoneNumber.trim()
                if (!Validators.isValidPhone(p)) throw ConflictException(ErrorCode.VALIDATION_FAILED, "Invalid phone number")
                if (p != user.phoneNumber && users.existsByPhoneNumber(p)) {
                    throw ConflictException(ErrorCode.PHONE_ALREADY_USED, "Phone number is already in use")
                }
                p
            }
            else -> user.phoneNumber
        }
        val updated = user.copy(
            fullName = command.fullName?.trim()?.takeIf { it.isNotBlank() } ?: user.fullName,
            photoUrl = command.photoUrl?.trim()?.takeIf { it.isNotBlank() } ?: user.photoUrl,
            phoneNumber = newPhone,
            phoneVerified = if (newPhone != user.phoneNumber) false else user.phoneVerified,
            dateOfBirth = command.dateOfBirth ?: user.dateOfBirth,
            updatedAt = clock.now(),
        )
        val saved = users.update(updated)
        audit.record(AuditAction.PROFILE_UPDATED, user.id, ctx)
        return saved
    }
}

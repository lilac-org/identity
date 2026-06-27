package id.andreasmlbngaol.identity.data.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Exposed table definitions. UUID primary keys are declared explicitly (rather
 * than via the DAO id tables) to keep the mapping fully under our control and
 * the data layer free of DAO leakage.
 */
object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 320).uniqueIndex()
    val username = varchar("username", 32).uniqueIndex()
    val passwordHash = varchar("password_hash", 255).nullable()
    val status = varchar("status", 32)
    val emailVerified = bool("email_verified").default(false)
    val fullName = varchar("full_name", 255).nullable()
    val photoUrl = varchar("photo_url", 1024).nullable()
    val phoneNumber = varchar("phone_number", 32).nullable().uniqueIndex()
    val phoneVerified = bool("phone_verified").default(false)
    val dateOfBirth = date("date_of_birth").nullable()
    val tokenVersion = integer("token_version").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object RolesTable : Table("roles") {
    val id = uuid("id")
    val name = varchar("name", 64).uniqueIndex()
    val description = varchar("description", 255).nullable()
    val isSystem = bool("is_system").default(false)
    override val primaryKey = PrimaryKey(id)
}

object PermissionsTable : Table("permissions") {
    val id = uuid("id")
    val name = varchar("name", 128).uniqueIndex()
    val description = varchar("description", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}

object RolePermissionsTable : Table("role_permissions") {
    val roleId = uuid("role_id").references(RolesTable.id)
    val permissionId = uuid("permission_id").references(PermissionsTable.id)
    override val primaryKey = PrimaryKey(roleId, permissionId)
}

object UserRolesTable : Table("user_roles") {
    val userId = uuid("user_id").references(UsersTable.id)
    val roleId = uuid("role_id").references(RolesTable.id)
    override val primaryKey = PrimaryKey(userId, roleId)
}

object RefreshTokensTable : Table("refresh_tokens") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val familyId = uuid("family_id").index()
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val clientId = varchar("client_id", 128).nullable()
    val issuedAt = timestamp("issued_at")
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
    val revokedAt = timestamp("revoked_at").nullable()
    val replacedByTokenId = uuid("replaced_by_token_id").nullable()
    val userAgent = varchar("user_agent", 512).nullable()
    val ipAddress = varchar("ip_address", 64).nullable()
    override val primaryKey = PrimaryKey(id)
}

object OAuthAccountsTable : Table("oauth_accounts") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val provider = varchar("provider", 32)
    val providerUserId = varchar("provider_user_id", 191)
    val email = varchar("email", 320).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(provider, providerUserId) }
}

object ClientsTable : Table("clients") {
    val id = uuid("id")
    val clientId = varchar("client_id", 128).uniqueIndex()
    val clientSecretHash = varchar("client_secret_hash", 255).nullable()
    val name = varchar("name", 191)
    val allowedAudiences = text("allowed_audiences").default("")
    val allowedScopes = text("allowed_scopes").default("")
    val redirectUris = text("redirect_uris").default("")
    val isConfidential = bool("is_confidential").default(true)
    val enabled = bool("enabled").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object EmailVerificationTokensTable : Table("email_verification_tokens") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object PasswordResetTokensTable : Table("password_reset_tokens") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object AuditLogsTable : Table("audit_logs") {
    val id = uuid("id")
    val action = varchar("action", 64).index()
    val userId = uuid("user_id").nullable().index()
    val clientId = varchar("client_id", 128).nullable()
    val ipAddress = varchar("ip_address", 64).nullable()
    val userAgent = varchar("user_agent", 512).nullable()
    val metadata = text("metadata").default("{}")
    val success = bool("success").default(true)
    val createdAt = timestamp("created_at").index()
    override val primaryKey = PrimaryKey(id)
}

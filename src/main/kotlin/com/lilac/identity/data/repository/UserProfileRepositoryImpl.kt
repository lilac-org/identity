package com.lilac.identity.data.repository

import com.lilac.identity.data.mapper.toDomain
import com.lilac.identity.data.model.UserProfileEntity
import com.lilac.identity.db.tables.UserProfilesTable
import com.lilac.identity.domain.model.CreateUserProfileException
import com.lilac.identity.domain.model.UserProfile
import com.lilac.identity.domain.repository.UserProfileRepository
import com.lilac.identity.util.toUUID
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class UserProfileRepositoryImpl(): UserProfileRepository {
    private fun ResultRow.toEntity() = UserProfileEntity(
        id = this[UserProfilesTable.id].value,
        userId = this[UserProfilesTable.userId],
        bio = this[UserProfilesTable.bio],
        profilePictureUrl = this[UserProfilesTable.profilePictureUrl],
        coverPictureUrl = this[UserProfilesTable.coverPictureUrl],
        createdAt = this[UserProfilesTable.createdAt],
        updatedAt = this[UserProfilesTable.updatedAt]
    )

    private fun UserProfilesTable.updateTimestamp(
        where: () -> Op<Boolean>,
        body: UserProfilesTable.(UpdateStatement) -> Unit
    ) = this
        .update(where) {
            it[updatedAt] = System.currentTimeMillis()
            body(it)
        }

    override fun findByUserId(userId: String): UserProfile? = transaction {
        UserProfilesTable
            .selectAll()
            .where { UserProfilesTable.userId eq userId.toUUID() }
            .map { it.toEntity() }
            .singleOrNull()
            ?.toDomain()
    }

    override fun create(
        userId: String,
        bio: String?,
        profilePictureUrl: String?,
        coverPictureUrl: String?
    ): String = transaction {
        try {
            val now = System.currentTimeMillis()

            UserProfilesTable
                .insertAndGetId {
                    it[UserProfilesTable.userId] = userId.toUUID()
                    it[UserProfilesTable.bio] = bio
                    it[UserProfilesTable.profilePictureUrl] = profilePictureUrl
                    it[UserProfilesTable.coverPictureUrl] = coverPictureUrl
                    it[UserProfilesTable.createdAt] = now
                    it[UserProfilesTable.updatedAt] = now
                }
                .value
                .toString()
        } catch (e: Exception) {
            throw CreateUserProfileException("Failed to create user profile: ${e.message}")
        }
    }

    override fun update(profile: UserProfile): Boolean = transaction {
        UserProfilesTable
            .updateTimestamp({ UserProfilesTable.userId eq profile.userId.toUUID() }) {
                it[bio] = profile.bio
                it[profilePictureUrl] = profile.profilePictureUrl
                it[coverPictureUrl] = profile.coverPictureUrl
                it[updatedAt] = profile.updatedAt
            } > 0
    }
}
package com.lilac.identity.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object UserProfilesTable: UUIDTable("user_profiles") {
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val bio = varchar("bio", 255).nullable()
    val profilePictureUrl = varchar("profile_picture_url", 255).nullable()
    val coverPictureUrl = varchar("cover_picture_url", 255).nullable()
    val createdAt = long("created_at").default(System.currentTimeMillis())
    val updatedAt = long("updated_at").default(System.currentTimeMillis())
}
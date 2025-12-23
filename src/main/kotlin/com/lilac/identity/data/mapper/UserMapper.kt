package com.lilac.identity.data.mapper

import com.lilac.identity.data.model.UserEntity
import com.lilac.identity.domain.model.User
import com.lilac.identity.util.toUUID

fun UserEntity.toDomain() = User(
    id = this.id.toString(),
    email = this.email,
    username = this.username,
    passwordHash = this.passwordHash,
    firstName = this.firstName,
    lastName = this.lastName,
    isEmailVerified = this.isEmailVerified,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

fun User.toEntity() = UserEntity(
    id = this.id.toUUID(),
    email = this.email,
    username = this.username,
    passwordHash = this.passwordHash,
    firstName = this.firstName,
    lastName = this.lastName,
    isEmailVerified = this.isEmailVerified,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
)
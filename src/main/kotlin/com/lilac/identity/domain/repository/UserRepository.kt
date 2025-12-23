package com.lilac.identity.domain.repository

import com.lilac.identity.domain.model.User

interface UserRepository {
    fun findById(id: String): User?
    fun findByEmail(email: String): User?
    fun findByEmailOrUsername(emailOrUsername: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
    fun create(
        email: String,
        username: String,
        passwordHash: String,
        firstName: String,
        lastName: String,
        isEmailVerified: Boolean = false
    ): String
    fun updatePassword(id: String, newHash: String): Boolean
    fun markEmailVerified(id: String): Boolean
}
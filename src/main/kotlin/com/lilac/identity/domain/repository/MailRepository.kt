package com.lilac.identity.domain.repository

interface MailRepository {
    fun sendEmailVerification(
        userId: String,
        email: String,
        fullName: String
    ): Boolean

    fun sendPasswordResetEmail(
        userId: String,
        email: String,
        fullName: String,
    ): Boolean

    fun sendWelcomeEmail(
        email: String,
        fullName: String
    ): Boolean
}
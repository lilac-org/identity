package com.lilac.identity.data.repository

import com.lilac.identity.domain.repository.MailRepository
import com.lilac.identity.domain.service.JwtService
import com.lilac.identity.domain.service.MailService

class MailRepositoryImpl(
    private val mailService: MailService,
    private val jwtService: JwtService
): MailRepository {
    override fun sendEmailVerification(userId: String, email: String, fullName: String): Boolean {
        return try {
            val token = jwtService.generateEmailVerificationToken(userId)
            val subject = "Verify your email"
            val link = "http://localhost:8080/verify-email?token=$token"
            val body = """
                <html>
                    <body>
                        <p>Hi $fullName,</p>
                        <p>Please verify your email by clicking the button below:</p>
                        <p><a href="$link" style="display:inline-block;padding:10px 20px;
                           background-color:#8e44ad;color:white;text-decoration:none;
                           border-radius:4px;">Verify Email</a></p>
                        <p>This link will expire in 10 minutes.</p>
                    </body>
                </html>
            """.trimIndent()

            mailService.send(
                to = email,
                subject = subject,
                html = body
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun sendPasswordResetEmail(
        userId: String,
        email: String,
        fullName: String
    ): Boolean {
        return try {
            val token = jwtService.generatePasswordResetToken(userId)
            val link = "http://localhost:8080/reset-password?token=$token"
            val subject = "Reset your password"

            val html = """
                <html><body>
                <p>Hi $fullName,</p>
                <p>Click below to reset your password:</p>
                <a href="$link" style="padding:10px 20px;
                    background-color:#8e44ad;color:white;text-decoration:none;
                    border-radius:4px;">Reset Password</a>
                </body></html>
            """.trimIndent()

            mailService.send(
                to = email,
                subject = subject,
                html = html
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun sendWelcomeEmail(email: String, fullName: String): Boolean {
        return try {
            val html = """
                <html><body>
                <p>Hi $fullName,</p>
                <p>Welcome to Lilac!</p>
                </body></html>
            """.trimIndent()

            mailService.send(email, "Welcome!", html)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
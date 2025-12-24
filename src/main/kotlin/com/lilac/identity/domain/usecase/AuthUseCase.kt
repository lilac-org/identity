package com.lilac.identity.domain.usecase

import com.lilac.identity.domain.model.*
import com.lilac.identity.domain.repository.MailRepository
import com.lilac.identity.domain.repository.UserProfileRepository
import com.lilac.identity.domain.repository.UserRepository
import com.lilac.identity.domain.service.JwtService
import com.lilac.identity.domain.service.PasswordService

class AuthUseCase(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val mailRepository: MailRepository,
    private val jwtService: JwtService,
    private val passwordService: PasswordService
) {
    fun registerUser(
        email: String,
        username: String,
        password: String,
        firstName: String,
        lastName: String
    ): TokenPair {
        val emailUsed = userRepository.existsByEmail(email)
        if (emailUsed) throw EmailAlreadyUsedException()

        val usernameUsed = userRepository.existsByUsername(username)
        if (usernameUsed) throw UsernameAlreadyUsedException()

        val passwordHash = passwordService.hash(password)

        val userId = userRepository.create(
            email = email,
            username = username,
            passwordHash = passwordHash,
            firstName = firstName,
            lastName = lastName,
        )

        userProfileRepository.create(
            userId = userId,
            bio = null,
            profilePictureUrl = null,
            coverPictureUrl = null
        )

        println("Successfully registered user: $userId")

        val emailSent = mailRepository.sendEmailVerification(
            userId = userId,
            email = email,
            fullName = "$firstName $lastName"
        )

        println("Email sent: $emailSent")
        if (!emailSent) {
            println("Failed to send email")
            userRepository.deleteById(userId)

            throw EmailVerificationNotSentException()
        }

        println("Successfully sent email")

        val user = userRepository.findById(userId) ?: throw UserNotFoundException()

        return generateTokenPair(user)
    }

    fun login(emailOrUsername: String, password: String): TokenPair {
        val user = userRepository.findByEmailOrUsername(emailOrUsername)
            ?: throw InvalidIdentifierException()

        passwordService.verify(password, user.passwordHash).let { verified ->
            if (!verified) throw InvalidIdentifierException()
        }

        return generateTokenPair(user)
    }

    fun verifyEmail(token: String): Boolean {
        val decoded = jwtService.decodeEmailVerificationToken(token) ?: throw InvalidTokenException()
        val userId = decoded.subject

        return userRepository.markEmailVerified(userId)
    }

    fun forgotPassword(emailOrUsername: String): Boolean {
        val user = userRepository.findByEmailOrUsername(emailOrUsername)
            ?: throw UserNotFoundException()

        return mailRepository.sendPasswordResetEmail(
            userId = user.id,
            email = user.email,
            fullName = "${user.firstName} ${user.lastName}"
        )
    }

    fun resetPassword(token: String, newPassword: String): Boolean {
        val decoded = jwtService.decodePasswordResetToken(token) ?: throw InvalidTokenException()
        val userId = decoded.subject
        val hash = passwordService.hash(newPassword)
        return userRepository.updatePassword(userId, hash)
    }

    private fun generateTokenPair(user: User): TokenPair {
        val accessToken = jwtService.generateAccessToken(
            userId = user.id,
            username = user.username,
            firstName = user.firstName,
            lastName = user.lastName,
            isEmailVerified = user.isEmailVerified
        )

        val refreshToken = jwtService.generateRefreshToken(user.id)

        return TokenPair(accessToken, refreshToken)
    }
}
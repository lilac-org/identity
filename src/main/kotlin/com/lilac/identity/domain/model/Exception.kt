package com.lilac.identity.domain.model

class InvalidIdentifierException(message: String = "Invalid Identifier or Password"): Exception(message)
class UserNotFoundException(message: String = "User not found"): Exception(message)

class EmailAlreadyUsedException(message: String = "Email already used"): Exception(message)
class UsernameAlreadyUsedException(message: String = "Username already used"): Exception(message)
class InvalidTokenException(message: String = "Invalid Token"): Exception(message)
class CreateUserException(message: String = "Failed to create user"): Exception(message)
class CreateUserProfileException(message: String = "Failed to create user profile"): Exception(message)
class InternalServerException(message: String = "Internal Server Error"): Exception(message)
class EmailVerificationNotSentException(message: String = "Email verification not sent"): Exception(message)
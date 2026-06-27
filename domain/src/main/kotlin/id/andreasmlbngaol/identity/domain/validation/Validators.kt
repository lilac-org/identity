package id.andreasmlbngaol.identity.domain.validation

import id.andreasmlbngaol.identity.domain.error.ValidationException

/**
 * Lightweight, dependency-free input validation living in the domain. Each
 * helper accumulates field errors and throws a single [ValidationException]
 * carrying a field -> message map, which the presentation layer renders.
 */
object Validators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_.]{3,32}$")
    // E.164-ish: optional +, 8-15 digits.
    private val PHONE_REGEX = Regex("^\\+?[1-9]\\d{7,14}$")

    class FieldErrors {
        private val errors = linkedMapOf<String, String>()
        fun add(field: String, message: String) { if (!errors.containsKey(field)) errors[field] = message }
        fun isNotEmpty() = errors.isNotEmpty()
        fun throwIfAny() { if (isNotEmpty()) throw ValidationException(fieldErrors = errors.toMap()) }
    }

    fun isValidEmail(value: String) = EMAIL_REGEX.matches(value.trim())
    fun isValidUsername(value: String) = USERNAME_REGEX.matches(value.trim())
    fun isValidPhone(value: String) = PHONE_REGEX.matches(value.trim())

    fun validateRegistration(
        email: String,
        username: String,
        password: String,
        phoneNumber: String?,
        minPasswordLength: Int,
        maxPasswordLength: Int,
    ) {
        val errors = FieldErrors()
        if (!isValidEmail(email)) errors.add("email", "Must be a valid email address")
        if (!isValidUsername(username)) {
            errors.add("username", "Must be 3-32 chars: letters, digits, underscore or dot")
        }
        validatePasswordStrength(password, minPasswordLength, maxPasswordLength, errors)
        if (phoneNumber != null && phoneNumber.isNotBlank() && !isValidPhone(phoneNumber)) {
            errors.add("phoneNumber", "Must be a valid phone number in E.164 format")
        }
        errors.throwIfAny()
    }

    fun validatePassword(password: String, minPasswordLength: Int, maxPasswordLength: Int) {
        val errors = FieldErrors()
        validatePasswordStrength(password, minPasswordLength, maxPasswordLength, errors)
        errors.throwIfAny()
    }

    private fun validatePasswordStrength(
        password: String,
        min: Int,
        max: Int,
        errors: FieldErrors,
    ) {
        when {
            password.length < min -> errors.add("password", "Must be at least $min characters")
            password.length > max -> errors.add("password", "Must be at most $max characters")
            !password.any { it.isDigit() } -> errors.add("password", "Must contain at least one digit")
            !password.any { it.isLetter() } -> errors.add("password", "Must contain at least one letter")
        }
    }
}

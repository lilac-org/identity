package com.lilac.identity.presentation.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val firstName: String,
    val lastName: String
)
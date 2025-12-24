package com.lilac.identity.presentation.response

import kotlinx.serialization.Serializable

@Serializable
data class HelloResponse(
    val success: Boolean = true,
    val message: String? = "Hello from Identity Service. Have a nice day!",
    val data: Nothing? = null
)
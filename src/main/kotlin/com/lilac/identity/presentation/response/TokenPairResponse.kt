package com.lilac.identity.presentation.response

import com.lilac.identity.presentation.dto.TokenPairDto
import kotlinx.serialization.Serializable

@Serializable
data class TokenPairResponse(
    val data: TokenPairDto,
    val success: Boolean = true,
    val message: String? = "Authentication successful"
)
package com.lilac.identity.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenPairDto(
    val accessToken: String,
    val refreshToken: String
)
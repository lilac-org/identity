package com.lilac.identity.domain.model

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)
package com.lilac.identity.presentation.mapper

import com.lilac.identity.domain.model.TokenPair
import com.lilac.identity.presentation.dto.TokenPairDto

fun TokenPair.toDto() = TokenPairDto(this.accessToken, this.refreshToken)
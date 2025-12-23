package com.lilac.identity.di

import com.lilac.identity.data.service.JwtServiceImpl
import com.lilac.identity.domain.service.JwtService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val securityModule = module {
    singleOf(::JwtServiceImpl).bind<JwtService>()
}
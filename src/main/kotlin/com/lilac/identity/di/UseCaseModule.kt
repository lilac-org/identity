package com.lilac.identity.di

import com.lilac.identity.domain.usecase.AuthUseCae
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    singleOf(::AuthUseCae)
}
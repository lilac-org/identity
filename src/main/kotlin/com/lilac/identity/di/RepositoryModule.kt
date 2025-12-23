package com.lilac.identity.di

import com.lilac.identity.data.repository.MailRepositoryImpl
import com.lilac.identity.data.repository.UserProfileRepositoryImpl
import com.lilac.identity.data.repository.UserRepositoryImpl
import com.lilac.identity.domain.repository.MailRepository
import com.lilac.identity.domain.repository.UserProfileRepository
import com.lilac.identity.domain.repository.UserRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::UserRepositoryImpl).bind<UserRepository>()
    singleOf(::UserProfileRepositoryImpl).bind<UserProfileRepository>()
    singleOf(::MailRepositoryImpl).bind<MailRepository>()
}
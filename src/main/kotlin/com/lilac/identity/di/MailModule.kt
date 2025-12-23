package com.lilac.identity.di

import com.lilac.identity.config.MailConfig
import com.lilac.identity.data.service.MailServiceImpl
import com.lilac.identity.domain.service.MailService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val mailModule = module {
    singleOf(::MailServiceImpl).bind<MailService>()
}
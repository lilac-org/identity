package com.lilac.identity.config

import com.lilac.identity.di.mailModule
import com.lilac.identity.di.securityModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    val appModule = module {
        single { loadMailConfig() }
        single { loadJwtConfig() }
    }

    install(Koin) {
        slf4jLogger()
        modules(
            appModule,
            mailModule,
            securityModule
        )
    }
}
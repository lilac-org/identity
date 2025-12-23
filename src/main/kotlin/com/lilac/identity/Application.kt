package com.lilac.identity

import com.lilac.identity.config.configureCors
import com.lilac.identity.config.configureHealth
import com.lilac.identity.config.configureKoin
import com.lilac.identity.config.configureLogging
import com.lilac.identity.config.configureResponse
import com.lilac.identity.config.configureRouting
import com.lilac.identity.config.configureSecurity
import com.lilac.identity.config.configureSerialization
import com.lilac.identity.db.DatabaseFactory.configureDatabase
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureKoin()
    configureLogging()
    configureSerialization()
    configureResponse()
    configureCors()
    configureSecurity()
    configureHealth()
    configureRouting()
}

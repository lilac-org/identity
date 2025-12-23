package com.lilac.identity.util

import io.ktor.server.application.Application

fun Application.getConfig(key: String) = environment.config.propertyOrNull(key)?.getString()


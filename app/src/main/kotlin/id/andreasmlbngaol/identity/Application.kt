package id.andreasmlbngaol.identity

import id.andreasmlbngaol.identity.config.ConfigLoader
import id.andreasmlbngaol.identity.data.db.DatabaseFactory
import id.andreasmlbngaol.identity.data.di.dataModule
import id.andreasmlbngaol.identity.data.security.RsaKeys
import id.andreasmlbngaol.identity.di.appModule
import id.andreasmlbngaol.identity.di.useCaseModule
import id.andreasmlbngaol.identity.domain.repository.UserRepository
import id.andreasmlbngaol.identity.presentation.di.ApiDependencies
import id.andreasmlbngaol.identity.presentation.installPresentation
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

/**
 * Process entry point. We construct configuration first (env / .env), then
 * start a Netty server bound to the configured host/port.
 */
fun main() {
    val config = ConfigLoader.load()
    embeddedServer(
        factory = Netty,
        port = config.server.port,
        host = config.server.host,
        module = { bootstrap() },
    ).start(wait = true)
}

/**
 * Composition root: install Koin, open the database (running migrations), then
 * hand the assembled [ApiDependencies] to the presentation layer.
 */
fun Application.bootstrap() {
    val config = ConfigLoader.load()

    install(Koin) {
        slf4jLogger()
        modules(appModule(config), dataModule(), useCaseModule())
    }

    val databaseFactory = get<DatabaseFactory>()
    databaseFactory.connect()
    monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        databaseFactory.close()
    }

    val deps = get<ApiDependencies>()
    val publicKey = get<RsaKeys>().publicKey
    val userRepository = get<UserRepository>()

    installPresentation(deps, publicKey, userRepository)
}

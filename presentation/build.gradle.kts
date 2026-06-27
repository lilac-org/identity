plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(25)
}

// The presentation layer handles HTTP concerns: routing, DTOs, plugins, the
// server-rendered admin dashboard. It depends ONLY on the domain module (it
// talks to use cases through domain interfaces) and must never see the data
// layer directly.
dependencies {
    implementation(project(":domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Ktor server
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.rateLimit)
    implementation(ktorLibs.server.requestValidation)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.compression)
    implementation(ktorLibs.server.openapi)
    implementation(ktorLibs.server.swagger)
    implementation(ktorLibs.server.htmlBuilder)
    implementation(ktorLibs.server.resources)

    implementation(libs.nimbus.jose.jwt)
    implementation(libs.kotlin.logging)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest.assertions)
    testImplementation(ktorLibs.server.testHost)
}

tasks.test {
    useJUnitPlatform()
}

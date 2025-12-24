import io.ktor.plugin.OpenApiPreview

plugins {
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.lilac"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.server.cors)
    implementation(libs.server.default.headers)
    implementation(libs.server.core)
    implementation(libs.server.auth)
    implementation(libs.server.auth.jwt)
    implementation(libs.server.request.validation)
    implementation(libs.server.resources)
    implementation(libs.server.host.common)
    implementation(libs.server.status.pages)
    implementation(libs.server.call.logging)
    implementation(libs.server.config.yaml)
    implementation(libs.server.content.negotiation)
    implementation(libs.server.netty)
    implementation(libs.server.openapi)
    implementation(libs.server.swagger)
    implementation(libs.serialization.json)
    implementation(libs.khealth)
    implementation(libs.logback)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikari.cp)

    implementation(libs.bcrypt)

    implementation(libs.jakarta.mail)

    implementation(libs.koin.ktor)
    implementation(libs.koin.slf4j)

    testImplementation(libs.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.client.content.negotiation)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
}

// Documentation Open API Generator Plugin
ktor {
    @OptIn(OpenApiPreview::class)
    openApi {
        title = "Identity API"
        version = "1.0"
        summary = "Identity API Summary"
        description = "Identity API Description"
        contact = "lgandre45@gmail.com"

        target = project.layout.projectDirectory
            .file("src/main/resources/openapi/documentation.json")
    }
}
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(25)
}

// The app module is the only place allowed to see EVERY layer. It wires the
// dependency-injection graph together and boots the Ktor server.
dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))

    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.config.yaml)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.dotenv)
    implementation(libs.kotlin.logging)
    implementation(libs.logback.classic)
    implementation(libs.logstash.encoder)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(ktorLibs.server.testHost)
}

application {
    mainClass = "id.andreasmlbngaol.identity.ApplicationKt"
}

// Run the service from the repository root so relative paths resolve there:
//   - `.env` (dotenv default directory is "./")
//   - JWT key files (JWT_PRIVATE_KEY_PATH=./keys/private.pem)
// Without this, `:app:run` uses the `app/` subproject dir as its working
// directory, the key files are not found, and RsaKeys fails with
// "Missing key encoding" (empty PEM).
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    // Silence the JDK 25 native-access warning emitted by JNA at runtime.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.test {
    useJUnitPlatform()
}

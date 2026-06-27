pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        // Official Ktor version catalog, exposed as `ktorLibs.*`
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.5.1")
    }
}

rootProject.name = "identity"

include(":domain")
include(":data")
include(":presentation")
include(":app")

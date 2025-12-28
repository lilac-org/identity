import io.ktor.plugin.OpenApiPreview

plugins {
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.lilac.identity"
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
    implementation(libs.server.html.builder)
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
    testImplementation(libs.h2)
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
            .file("src/main/resources/openapi/generated.json")
    }
}

tasks.register("convertOpenApi") {
    dependsOn("buildOpenApi")

    doLast {
        val inputFile = file("src/main/resources/openapi/generated.json")
        val outputFile = file("src/main/resources/openapi/documentation.json")

        if (!inputFile.exists()) {
            println("❌ File not found: ${inputFile.absolutePath}")
            return@doLast
        }

        try {
            val jsonString = inputFile.readText()

            // Parse manual tanpa library (simple parsing)
            val converted = convertCamelCaseToSnakeCase(jsonString)

            outputFile.parentFile?.mkdirs()
            outputFile.writeText(converted)

            if (inputFile.delete()) {
                println("🗑️ generated.json successfully deleted")
            } else {
                println("⚠️ Failed to delete generated.json")
            }

            println("✅ Successfully convert!")
            println("📁 Output: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }
}

// Simple string-based conversion tanpa perlu import serialization
fun convertCamelCaseToSnakeCase(jsonString: String): String {
    val result = StringBuilder()
    var inString = false
    var escapeNext = false
    var insideProperties = false
    var braceDepth = 0
    var propertiesBraceDepth = -1

    val keyBuffer = StringBuilder()
    var capturingKey = false

    for (i in jsonString.indices) {
        val c = jsonString[i]

        when {
            escapeNext -> {
                result.append(c)
                escapeNext = false
            }

            c == '\\' && inString -> {
                result.append(c)
                escapeNext = true
            }

            c == '"' -> {
                inString = !inString
                result.append(c)

                if (inString) {
                    keyBuffer.clear()
                    capturingKey = true
                } else {
                    capturingKey = false

                    val key = keyBuffer.toString()

                    // Deteksi masuk properties
                    if (key == "properties") {
                        insideProperties = true
                    }

                    // Konversi HANYA kalau di dalam properties
                    if (insideProperties && key.matches(Regex("[a-z][a-zA-Z0-9]*"))) {
                        val snake = key.replace(Regex("[A-Z]")) {
                            "_${it.value.lowercase()}"
                        }
                        result.setLength(result.length - key.length - 1)
                        result.append(snake).append('"')
                    }
                }
            }

            inString && capturingKey -> {
                keyBuffer.append(c)
                result.append(c)
            }

            !inString -> {
                when (c) {
                    '{' -> {
                        braceDepth++
                        if (insideProperties && propertiesBraceDepth == -1) {
                            propertiesBraceDepth = braceDepth
                        }
                        result.append(c)
                    }

                    '}' -> {
                        if (insideProperties && braceDepth == propertiesBraceDepth) {
                            insideProperties = false
                            propertiesBraceDepth = -1
                        }
                        braceDepth--
                        result.append(c)
                    }

                    else -> result.append(c)
                }
            }

            else -> result.append(c)
        }
    }

    return formatJson(result.toString())
}

fun formatJson(json: String): String {
    val result = StringBuilder()
    var indent = 0
    var inString = false
    var escapeNext = false

    for (char in json) {
        when {
            escapeNext -> {
                result.append(char)
                escapeNext = false
            }
            char == '\\' && inString -> {
                result.append(char)
                escapeNext = true
            }
            char == '"' -> {
                result.append(char)
                inString = !inString
            }
            !inString -> {
                when (char) {
                    '{', '[' -> {
                        result.append(char)
                        indent++
                        result.append("\n").append("  ".repeat(indent))
                    }
                    '}', ']' -> {
                        indent--
                        result.append("\n").append("  ".repeat(indent))
                        result.append(char)
                    }
                    ',' -> {
                        result.append(char)
                        result.append("\n").append("  ".repeat(indent))
                    }
                    ':' -> {
                        result.append(char).append(" ")
                    }
                    ' ', '\n', '\t' -> {
                        // skip whitespace
                    }
                    else -> result.append(char)
                }
            }
            else -> result.append(char)
        }
    }

    return result.toString()
}

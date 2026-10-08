
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
}

group = "com.movofeeds"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.logback.classic)

    implementation(ktorLibs.server.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.postgresql)
    implementation(libs.bcrypt)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(libs.jwt)
    // HTTP client for API calls (Ktor client — you already have this for the server)
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.okhttp)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.callLogging)
}
tasks.test {
    environment(
        "JWT_SECRET",
        "test-secret-for-unit-tests-only"
    )
}
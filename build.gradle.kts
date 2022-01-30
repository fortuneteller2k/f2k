plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

group = "com.f2k"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val exposedVersion: String by project
val jacksonVersion: String by project

dependencies {
    implementation("net.dv8tion", "JDA", "5.0.0-alpha.5") {
        exclude(module = "opus-java")
    }

    implementation("com.github.minndevelopment", "jda-ktx", "d3c6b4d")
    implementation("io.github.cdimascio", "dotenv-kotlin", "6.2.2")
    implementation("org.jetbrains.kotlin", "kotlin-stdlib")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")
    implementation("org.xerial", "sqlite-jdbc", "3.36.0.3")
    implementation("org.scilab.forge", "jlatexmath", "1.0.7")
    implementation("org.apache.commons", "commons-lang3", "3.12.0")
    implementation("org.apache.commons", "commons-collections4", "4.4")
    implementation("com.squareup.okhttp3", "okhttp", "4.9.3")
    implementation("ch.qos.logback", "logback-classic", "1.3.0-alpha12")
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("com.fasterxml.jackson.core", "jackson-core", jacksonVersion)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)
}
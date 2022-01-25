plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

group = "com.f2k"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-alpha.4") {
        exclude(module = "opus-java")
    }

    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("com.github.minndevelopment:jda-ktx:d3c6b4d")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha12")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("io.github.classgraph:classgraph:4.8.138")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jetbrains.exposed:exposed-core:0.37.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.37.3")
}
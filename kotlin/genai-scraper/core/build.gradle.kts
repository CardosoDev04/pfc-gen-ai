plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.cardoso-solutions"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":llm"))
    implementation(project(":healthcheck"))
    implementation(project(":reporting"))
    implementation(project(":scrapers"))
    implementation(project(":common_services"))
    implementation ("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.seleniumhq.selenium:selenium-java:4.29.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.29.0")
    implementation ("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("org.junit.platform:junit-platform-launcher:1.9.3")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    testImplementation("org.junit.platform:junit-platform-launcher:1.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
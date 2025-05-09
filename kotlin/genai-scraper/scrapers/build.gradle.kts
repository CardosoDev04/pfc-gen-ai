plugins {
    kotlin("jvm")
}

group = "com.cardoso-solutions"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":common_services"))
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("org.seleniumhq.selenium:selenium-java:4.29.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.29.0")
    implementation ("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
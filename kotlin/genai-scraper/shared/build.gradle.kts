plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.10"
}

group = "com.cardoso-solutions"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.seleniumhq.selenium:selenium-java:4.29.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.29.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
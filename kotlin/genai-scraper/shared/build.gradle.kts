plugins {
    kotlin("jvm")
}

group = "com.cardoso-solutions"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.seleniumhq.selenium:selenium-java:4.29.0")
}

kotlin {
    jvmToolchain(21)
}
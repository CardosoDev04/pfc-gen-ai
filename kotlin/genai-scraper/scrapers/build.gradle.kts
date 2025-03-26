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
    implementation(project(":shared"))
    implementation("org.seleniumhq.selenium:selenium-java:4.29.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.29.0")
    implementation ("io.github.cdimascio:dotenv-kotlin:6.2.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "genai-scraper"
include("core")
include("domain")
include("llm")
include("scrapers")
include("healthcheck")
include("common_services")

package library.builder

import classes.llm.LLM
import compiler.ScraperCompiler
import html_fetcher.WebExtractor
import modification_detection.IModificationDetectionService
import org.openqa.selenium.WebDriver
import persistence.PersistenceService
import snapshots.ISnapshotService
import wrapper.Scraper

class ScraperBuilder {

    private var modificationDetectionService: IModificationDetectionService? = null
    private var snapshotService: ISnapshotService? = null
    private var webExtractor: WebExtractor? = null
    private var persistenceService: PersistenceService? = null
    private var driver: WebDriver? = null
    private var scraperTestClassName: String? = null
    private var retries: Int = 3
    private var model: LLM = LLM.Mistral7B

    fun withModificationDetectionService(service: IModificationDetectionService) = apply {
        this.modificationDetectionService = service
    }

    fun withSnapshotService(service: ISnapshotService) = apply {
        this.snapshotService = service
    }

    fun withWebExtractor(extractor: WebExtractor) = apply {
        this.webExtractor = extractor
    }

    fun withPersistenceService(service: PersistenceService) = apply {
        this.persistenceService = service
    }

    fun withWebDriver(driver: WebDriver) = apply {
        this.driver = driver
    }

    fun withScraperTestClassName(className: String) = apply {
        this.scraperTestClassName = className
    }

    fun withRetries(retries: Int) = apply {
        this.retries = retries
    }

    fun withModel(model: LLM) = apply {
        this.model = model
    }

    suspend fun build(driver: WebDriver, scraperPath: String): Scraper {

        val compiled = ScraperCompiler.attemptToCompileAndInstantiate(
            driver = driver,
            snapshotService = snapshotService ?: error("snapshotService not set"),
            scraperCodePath = scraperPath
        ) ?: error("Initial scraper compilation failed.")

        return Scraper(
            modificationDetectionService = modificationDetectionService ?: error("modificationDetectionService not set"),
            snapshotService = snapshotService!!,
            webExtractor = webExtractor ?: error("webExtractor not set"),
            persistenceService = persistenceService ?: error("persistenceService not set"),
            driver = driver,
            scraperTestClassName = scraperTestClassName ?: error("scraperTestClassName not set"),
            backupScraper = null,
            currentScraper = compiled,
            retries = retries,
            model = model
        )
    }

}

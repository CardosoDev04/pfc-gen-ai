package library.builder

import classes.llm.LLM
import compiler.ScraperCompiler
import html_fetcher.WebExtractor
import interfaces.IScraper
import modification_detection.IModificationDetectionService
import org.openqa.selenium.WebDriver
import persistence.PersistenceService
import snapshots.ISnapshotService
import wrapper.Scraper
import kotlin.reflect.KClass

class ScraperBuilder {

    private var modificationDetectionService: IModificationDetectionService? = null
    private var snapshotService: ISnapshotService? = null
    private var webExtractor: WebExtractor? = null
    private var persistenceService: PersistenceService? = null
    private var scraperTestClazz: KClass<*>? = null
    private var retries: Int = 3
    private var model: LLM = LLM.Mistral7B
    private var driver: WebDriver? = null

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

    fun withScraperTestClassName(clazz: KClass<*>) = apply {
        this.scraperTestClazz = clazz
    }

    fun withRetries(retries: Int) = apply {
        this.retries = retries
    }

    fun withModel(model: LLM) = apply {
        this.model = model
    }

    fun withDriver(driver: WebDriver) = apply {
        this.driver = driver
    }

    fun build(initialScraper: IScraper): Scraper {

        return Scraper(
            modificationDetectionService = modificationDetectionService ?: error("modificationDetectionService not set"),
            snapshotService = snapshotService!!,
            webExtractor = webExtractor ?: error("webExtractor not set"),
            persistenceService = persistenceService ?: error("persistenceService not set"),
            backupScraper = null,
            scraperTestClazz = scraperTestClazz ?: error("scraperTestClazz not set"),
            currentScraper = initialScraper,
            retries = retries,
            model = model,
            driver = driver ?: error("driver not set")
        )
    }

}

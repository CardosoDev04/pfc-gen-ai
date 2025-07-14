package library.builder

import classes.llm.LLM
import html_fetcher.WebExtractor
import interfaces.IScraper
import modification_detection.IModificationService
import org.openqa.selenium.WebDriver
import persistence.PersistenceService
import snapshots.ISnapshotService
import wrapper.Scraper
import kotlin.reflect.KClass

class ScraperBuilder {

    private var scraper: IScraper? = null
    private var modificationDetectionService: IModificationService? = null
    private var snapshotService: ISnapshotService? = null
    private var webExtractor: WebExtractor? = null
    private var persistenceService: PersistenceService? = null
    private var retries: Int = 3
    private var model: LLM = LLM.Mistral7B
    private var driver: WebDriver? = null

    fun withScraper(scraper: IScraper) = apply {
        this.scraper = scraper
    }

    fun withModificationDetectionService(service: IModificationService) = apply {
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

    fun withRetries(retries: Int) = apply {
        this.retries = retries
    }

    fun withScriptRepairModel(model: LLM) = apply {
        this.model = model
    }

    fun withDriver(driver: WebDriver) = apply {
        this.driver = driver
    }

    fun build(scraperKlass: KClass<*>): Scraper {
        return Scraper(
            scraper = scraper ?: error("Scraper not set"),
            modificationService = modificationDetectionService ?: error("ModificationDetectionService not set"),
            snapshotService = snapshotService!!,
            webExtractor = webExtractor ?: error("WebExtractor not set"),
            persistenceService = persistenceService ?: error("PersistenceService not set"),
            scraperKlass = scraperKlass,
            maxRetries = retries,
            model = model,
            driver = driver ?: error("Driver not set")
        )
    }

}

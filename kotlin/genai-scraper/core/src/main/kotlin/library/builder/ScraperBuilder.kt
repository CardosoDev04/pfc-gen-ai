package library.builder

import classes.llm.LLM
import domain.prompts.FEW_SHOT_GET_MODIFICATION_PROMPT
import kotlinx.coroutines.runBlocking
import modification_detection.ModificationDetectionService
import okhttp3.OkHttpClient
import ollama.OllamaClient
import org.openqa.selenium.WebDriver
import persistence.implementations.FilePersistenceService
import snapshots.SnapshotService
import html_fetcher.WebExtractor
import library.wrappers.GenericScraper
import orchestrator.Orchestrator
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class ScraperBuilder {
    private var retries: Int = 3
    private var model: String = LLM.Mistral7B.modelName
    private val httpCli = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val llmCli = OllamaClient(httpCli)

    fun withRetries(retries: Int): ScraperBuilder {
        this.retries = retries
        return this
    }

    fun withModel(model: String): ScraperBuilder {
        this.model = model
        return this
    }

    fun build(driver: WebDriver, scraperFilePath: String, scraperTestClass: KClass<*>): GenericScraper {
        val orchestrator = Orchestrator(
            modificationDetectionService = ModificationDetectionService(
                llmClient = llmCli,
                getModificationModel = model,
                getModificationMessageHistory = FEW_SHOT_GET_MODIFICATION_PROMPT
            ),
            snapshotService = SnapshotService(),
            webExtractor = WebExtractor(),
            persistenceService = FilePersistenceService(),
            driver = driver,
            scraperTestKlass = scraperTestClass
        )

        val compiled = runBlocking { orchestrator.attemptToCompileAndInstantiateNewScraper(scraperFilePath) }
            ?: throw IllegalStateException("Failed to compile and instantiate the scraper")

        return GenericScraper(
            scraperInstance = compiled.scraper,
            classLoader = compiled.classLoader,
            orchestrator = orchestrator,
            scraperName = scraperFilePath.split("/").last().substringBeforeLast("."),
            scraperPath = scraperFilePath,
            retries = retries,
        )
    }
}
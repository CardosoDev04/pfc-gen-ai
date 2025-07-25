import classes.llm.LLM
import com.cardoso.common.buildChromeDriver
import html_fetcher.WebExtractor
import kotlinx.coroutines.runBlocking
import library.builder.ScraperBuilder
import modification_detection.ModificationService
import okhttp3.OkHttpClient
import ollama.OllamaClient
import persistence.implementations.FilePersistenceService
import snapshots.SnapshotService
import scraper.DemoScraper
import java.util.concurrent.TimeUnit


fun main() {
    runBlocking {
        val snapshotService = SnapshotService(DemoScraper::class.simpleName ?: "")
        val driver = buildChromeDriver()
        val persistenceService = FilePersistenceService()
        val webExtractor = WebExtractor()
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
        val llmClient = OllamaClient(httpClient)
        val modificationDetectionService = ModificationService(llmClient, LLM.Gemma3_12B.modelName)

        val scraper = ScraperBuilder()
            .withScraper(DemoScraper(driver, snapshotService))
            .withScriptRepairModel(LLM.Gemma3_12B)
            .withRetries(3)
            .withSnapshotService(snapshotService)
            .withPersistenceService(persistenceService)
            .withWebExtractor(webExtractor)
            .withModificationDetectionService(modificationDetectionService)
            .withDriver(driver)
            .build(DemoScraper::class)

        try {
            scraper.scrape()
        } catch (e: Exception) {
            println("Error during scraping: ${e.message}")
            e.printStackTrace()
        }
    }
}

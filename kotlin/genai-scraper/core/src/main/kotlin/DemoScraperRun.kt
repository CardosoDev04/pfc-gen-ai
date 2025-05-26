import classes.llm.LLM
import com.cardoso.common.buildChromeDriver
import html_fetcher.WebExtractor
import kotlinx.coroutines.runBlocking
import library.builder.ScraperBuilder
import modification_detection.ModificationDetectionService
import okhttp3.OkHttpClient
import ollama.OllamaClient
import persistence.implementations.FilePersistenceService
import scrapers.DemoScraper
import snapshots.SnapshotService
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
        val modificationDetectionService = ModificationDetectionService(llmClient, LLM.Gemma3_4B.modelName)

        val initialScraper = DemoScraper(driver, snapshotService)

        val scraper = ScraperBuilder()
            .withModel(LLM.CodeLlama7B)
            .withRetries(3)
            .withSnapshotService(snapshotService)
            .withPersistenceService(persistenceService)
            .withWebExtractor(webExtractor)
            .withScraperTestClassName("DemoScraperTest")
            .withModificationDetectionService(modificationDetectionService)
            .withDriver(driver)
            .build(initialScraper)

        try {
            scraper.scrape()
        } catch (e: Exception) {
            println("Error during scraping: ${e.message}")
            e.printStackTrace()
        }
    }
}
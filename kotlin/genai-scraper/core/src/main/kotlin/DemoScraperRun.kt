import classes.llm.LLM
import com.cardoso.common.buildChromeDriver
import domain.prompts.FEW_SHOT_GET_MODIFICATION_PROMPT
import html_fetcher.WebExtractor
import kotlinx.coroutines.runBlocking
import library.builder.ScraperBuilder
import modification_detection.ModificationDetectionService
import okhttp3.OkHttpClient
import ollama.OllamaClient
import persistence.implementations.FilePersistenceService
import snapshots.SnapshotService
import java.util.concurrent.TimeUnit

fun main() {
    runBlocking {
        val driver = buildChromeDriver()
        val snapshotService = SnapshotService()
        val persistenceService = FilePersistenceService()
        val webExtractor = WebExtractor()
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
        val llmClient = OllamaClient(httpClient)
        val modificationDetectionService = ModificationDetectionService(llmClient, LLM.Gemma3_4B.modelName, FEW_SHOT_GET_MODIFICATION_PROMPT)


        val scraper = ScraperBuilder()
            .withModel(LLM.CodeLlama7B)
            .withRetries(3)
            .withSnapshotService(snapshotService)
            .withWebDriver(driver)
            .withPersistenceService(persistenceService)
            .withWebExtractor(webExtractor)
            .withScraperTestClassName("scrapers.DemoScraperTest")
            .withModificationDetectionService(modificationDetectionService)
            .build(
                driver,
                scraperPath = Configurations.scrapersBaseDir + "DemoScraper.kt"
            )

        try {
            scraper.scrape()
        } catch (e: Exception) {
            println("Error during scraping: ${e.message}")
            e.printStackTrace()
        } finally {
            scraper.close()
        }
    }
}
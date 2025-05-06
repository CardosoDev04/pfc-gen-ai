import classes.llm.LLM
import com.cardoso.common.buildChromeDriver
import kotlinx.coroutines.runBlocking
import library.builder.ScraperBuilder
import scrapers.DemoScraperTest

fun main() {
    val driver = buildChromeDriver()

    val testClass = DemoScraperTest::class

    val scraper = ScraperBuilder()
        .withModel(LLM.Mistral7B.modelName)
        .withRetries(3)
        .build(
            driver,
            Configurations.scrapersBaseDir + "DemoScraper.kt",
            testClass
        )

    runBlocking {
        try {
            scraper.scrapeWithRetries()
        } catch (e: Exception) {
            println("Error during scraping: ${e.message}")
        } finally {
            scraper.close()
        }
    }
}
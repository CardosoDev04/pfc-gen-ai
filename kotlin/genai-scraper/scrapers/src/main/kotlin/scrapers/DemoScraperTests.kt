package scrapers

import interfaces.IScraper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import snapshots.ISnapshotService
import snapshots.SnapshotService

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoScraperTest {

    private lateinit var driver: WebDriver
    private lateinit var scraper: IScraper
    private lateinit var snapshotService: ISnapshotService

    @BeforeAll
    fun setUp() {
        // Setup headless Chrome
        val options = ChromeOptions().apply {
            addArguments("--headless=new", "--disable-gpu", "--no-sandbox")
        }
        driver = ChromeDriver(options)

        // Real or stub snapshot service
        snapshotService = SnapshotService()
    }

    /**
     * Method to inject the scraper instance for testing.
     */
    fun setScraper(scraper: IScraper) {
        this.scraper = scraper
    }

    @Test
    fun `scrape should return list of BookingOption from real page`() {
        runBlocking {
            // Replace this with the actual URL your scraper should visit
            val targetUrl = "http://localhost:5173"

            driver.get(targetUrl)

            val result = (scraper as DemoScraper).scrape()

            // Replace this with the expected result from the real page
            result.forEach { println(it) }

            // A basic assertion to check non-empty result
            Assertions.assertFalse(result.isEmpty(), "Expected non-empty booking options")
        }
    }

    @AfterAll
    fun tearDown() {
        driver.quit()
    }
}

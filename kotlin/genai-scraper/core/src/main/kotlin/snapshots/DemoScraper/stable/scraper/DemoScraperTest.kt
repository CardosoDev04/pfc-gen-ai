package scraper

import com.cardoso.common.buildChromeDriver
import interfaces.IScraper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.openqa.selenium.WebDriver
import scraper.DemoScraper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoScraperTest(val scraper: IScraper) {

    private val driver: WebDriver = buildChromeDriver(true)

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

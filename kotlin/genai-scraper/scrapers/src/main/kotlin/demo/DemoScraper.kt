package demo

import classes.data.BookingOption
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import com.cardoso.common.buildChromeDriver
import org.openqa.selenium.TimeoutException
import snapshots.ISnapshotService
import snapshots.SnapshotService
import java.time.Duration

class DemoScraper(private val driver: WebDriver, private val snapshotService: ISnapshotService) : IDemoScraper {
    override fun getBookingOptions(): List<BookingOption> {
        val webDriverWait = WebDriverWait(driver, Duration.ofSeconds(5))
        try {
            driver.get("http://localhost:5173/")

            webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("search-button"))).click()

            val optionElements = webDriverWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("item-title")))

            val results = optionElements.map { BookingOption(it.text) }

            return results
        } catch (e: Exception) {
            if(e is NoSuchElementException || e is TimeoutException) {
                snapshotService.takeSnapshotAsFile(driver, "kotlin/genai-scraper/healthcheck/src/main/kotlin/snapshots/saved/demo_website/latest")
            }
            throw e
        }
    }

    override fun bookTrip(from: String, to: String, optionTitle: String) {
        val webDriverWait = WebDriverWait(driver, Duration.ofSeconds(5))

        driver.get("http://localhost:5173/")
        driver.findElement(By.id("origin-input")).sendKeys(from)
        driver.findElement(By.id("destination-input")).sendKeys(to)
        driver.findElement(By.id("search-button")).click()

        webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("result")))

        val targetItem = driver.findElements(By.cssSelector("div.result")).find { resultDiv ->
            val titleElement = resultDiv.findElement(By.tagName("h1"))
            titleElement.text == optionTitle
        }

        targetItem?.findElement(By.className("book-btn"))?.click()
    }
}

fun main() {
    val driver = buildChromeDriver(true)
    val snapshotService = SnapshotService()
    val scraper = DemoScraper(driver, snapshotService)

    val options = scraper.getBookingOptions()
    println(options)
    // scraper.bookTrip("Lisboa", "Rome", "Item 3")
}
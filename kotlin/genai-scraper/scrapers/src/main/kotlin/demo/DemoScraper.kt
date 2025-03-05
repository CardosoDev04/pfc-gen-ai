package demo

import classes.BookingOption
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import com.cardoso.common.buildChromeDriver
import interfaces.IDemoScraper
import java.time.Duration

class DemoScraper(private val driver: WebDriver): IDemoScraper {
    override fun getBookingOptions(): List<BookingOption> {
        val webDriverWait = WebDriverWait(driver, Duration.ofSeconds(5))

        driver.get("http://localhost:5173/")

        driver.findElement(By.id("search-button")).click()

        val optionElements = driver.findElements(By.id("item-title"))

        val results = optionElements.map { it -> BookingOption(it.text) }

        return results
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
    val driver = buildChromeDriver()
    val scraper = DemoScraper(driver)

    val options = scraper.getBookingOptions()
    println(options)
    scraper.bookTrip("Lisboa", "Rome", "Item 3")
}
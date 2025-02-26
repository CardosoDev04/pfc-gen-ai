package scraper.demo

import domain.classes.BookingOption
import domain.interfaces.IDemoScraper
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import scraper.common.buildChromeDriver
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
        TODO("Not yet implemented")
    }
}

fun main() {
    val driver = buildChromeDriver()
    val scraper = DemoScraper(driver)

    val options = scraper.getBookingOptions()
    println(options)
}
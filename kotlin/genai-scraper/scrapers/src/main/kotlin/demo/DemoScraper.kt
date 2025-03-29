package demo

import classes.data.BookingOption
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import com.cardoso.common.buildChromeDriver
import interfaces.IDemoScraper
import snapshots.ISnapshotService
import snapshots.SnapshotService
import java.time.Duration

class DemoScraper(private val driver: WebDriver, private val snapshotService: ISnapshotService) : IDemoScraper {
    override fun getBookingOptions(): List<BookingOption> {
        try {
            val webDriverWait = WebDriverWait(driver, Duration.ofSeconds(5))
            driver.get("http://localhost:5173/")

            snapshotService.takeSnapshotAsFile(
                driver,
                "/Users/joaocardoso/Documents/Faculdade/PFC/pfc-gen-ai/kotlin/genai-scraper/core/src/main/kotlin/snapshots/demo_website/get_options/latest/step1"
            )

            webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("search-button"))).click()


            val optionElements =
                webDriverWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("item-title")))

            snapshotService.takeSnapshotAsFile(
                driver,
                "/Users/joaocardoso/Documents/Faculdade/PFC/pfc-gen-ai/kotlin/genai-scraper/core/src/main/kotlin/snapshots/demo_website/get_options/latest/step2"
            )


            val results = optionElements.map { BookingOption(it.text) }

            return results
        } catch (e: Exception) {
            snapshotService.takeSnapshotAsFile(driver, "kotlin/working/snapshots/demo_website/get_options/latest")
            throw e
        }
    }
}
package scrapers

import Configurations
import classes.data.BookingOption
import classes.scrapers.DemoScraperDataBundle
import interfaces.IScraper
import interfaces.IScraperData
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import snapshots.ISnapshotService
import steptracker.StepTracker
import java.time.Duration

class DemoScraper(private val driver: WebDriver, private val snapshotService: ISnapshotService) : IScraper {
    override suspend fun scrape(): List<BookingOption> {
        try {
            val identifier = StepTracker.initializeRun()

            val webDriverWait = WebDriverWait(driver, Duration.ofSeconds(5))
            driver.get("http://localhost:5173/")

            snapshotService.takeSnapshotAsFile(driver, Configurations.snapshotBaseDir + "${this::class.simpleName}/latest/step1")
            StepTracker.incrementStep(identifier)

            webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("search-button"))).click()

            val optionElements = webDriverWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("item-title")))

            snapshotService.takeSnapshotAsFile(driver, Configurations.snapshotBaseDir + "${this::class.simpleName}/latest/step2")
            StepTracker.incrementStep(identifier)

            val results = optionElements.map { BookingOption(it.text) }

            return results
        } catch (e: Exception) {
            snapshotService.takeSnapshotAsFile(driver, "kotlin/working/snapshots/${this::class.simpleName}/latest")
            throw e
        }
    }

    override fun getScraperData(): IScraperData = DemoScraperDataBundle(
        path = Configurations.scrapersBaseDir + this::class.simpleName + ".kt",
        compiledClass = this
    )
}
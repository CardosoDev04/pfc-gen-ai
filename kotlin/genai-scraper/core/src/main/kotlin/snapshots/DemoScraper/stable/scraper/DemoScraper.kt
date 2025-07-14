package scraper

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
        val identifier = StepTracker.initializeRun()

        val webDriverWait = WebDriverWait(driver, Duration.ofSeconds(5))
        driver.get("http://localhost:5173/")

        snapshotService.takeSnapshotAsFile(driver)
        val originInput = webDriverWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#origin-input")))
        originInput.sendKeys("New York")
        StepTracker.incrementStep(identifier)

        snapshotService.takeSnapshotAsFile(driver)
        val destinationInput =
            webDriverWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#destination-input")))
        destinationInput.sendKeys("Los Angeles")
        StepTracker.incrementStep(identifier)

        val searchButton = webDriverWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#search-button")))
        searchButton.click()
        StepTracker.incrementStep(identifier)

        snapshotService.takeSnapshotAsFile(driver)
        val optionElements =
            webDriverWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("h1#item-titulo")))
        StepTracker.incrementStep(identifier)

        val results = optionElements.map { BookingOption(it.text) }

        return results
    }

    override fun getScraperData(): IScraperData = DemoScraperDataBundle(
        path = Configurations.snapshotBaseDir + "stable/" + this::class.simpleName + ".kt",
        compiledClass = this
    )
}
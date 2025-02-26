package scraper.common

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

fun buildChromeDriver(runInHeadlessMode: Boolean = false): WebDriver {
    val options = ChromeOptions()
    options.addArguments(
        "--no-sandbox",
        "--disable-gpu",
        "--disable-dev-shm-usage",
        "--disable-blink-features=AutomationControlled"
    )

    if (runInHeadlessMode)
        options.addArguments("--headless")

    val driver = ChromeDriver(options)
    driver.manage().window().maximize()
    return driver
}
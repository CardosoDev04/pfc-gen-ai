package library.wrappers

import Configurations
import classes.scrapers.GenericScraperDataBundle
import domain.model.interfaces.IOrchestrator
import interfaces.IScraper
import interfaces.IScraperData
import okio.Closeable
import java.net.URLClassLoader

class GenericScraper internal constructor(
    private val orchestrator: IOrchestrator,
    private val scraperName: String,
    private var scraperInstance: IScraper?,
    private var classLoader: URLClassLoader?,
    private var scraperPath: String,
    private var retries: Int = 0
): Closeable {

    private suspend fun scrape(): Boolean {
        return orchestrator.runScraper(this, Configurations.snapshotBaseDir + "$scraperName/latest")
    }

    suspend fun scrapeWithRetries(): Boolean {
        var attempts = 0
        var success = false
        while (!success && attempts < retries) {
            success = scrape()
            if (!success) {
                println("Retry #${attempts + 1} failed.")
            }
            attempts++
        }

        if (!success) {
            println("Max retries reached.")
        }

        return success
    }

    fun getScraperInstance(): IScraper = scraperInstance ?: throw IllegalStateException("Scraper was not initialized")

    fun getClassLoader(): URLClassLoader = classLoader ?: throw IllegalStateException("Classloader was not initialized")

    fun setScraperInstance(newScraper: IScraper, newClassLoader: URLClassLoader) {
        close()
        scraperInstance = newScraper
        classLoader = newClassLoader
    }

    fun getScraperData(): IScraperData {
        return GenericScraperDataBundle(scraperPath, getScraperInstance())
    }

    override fun close() {
        try {
            scraperInstance = null
            classLoader?.close()
        } catch (e: Exception) {
            println("Error closing classloader: \${e.message}")
        } finally {
            classLoader = null
        }
    }
}
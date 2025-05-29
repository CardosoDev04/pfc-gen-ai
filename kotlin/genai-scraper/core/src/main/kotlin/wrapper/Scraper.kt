package wrapper

import Configurations
import classes.llm.LLM
import compiler.CompiledScraperResult
import compiler.ScraperCompiler
import domain.model.interfaces.IScraperWrapper
import html_fetcher.WebExtractor
import interfaces.IScraper
import modification_detection.IModificationDetectionService
import org.openqa.selenium.*
import persistence.PersistenceService
import snapshots.ISnapshotService
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class Scraper(
    private val modificationDetectionService: IModificationDetectionService,
    private val snapshotService: ISnapshotService,
    private val webExtractor: WebExtractor,
    private val persistenceService: PersistenceService,
    private val driver: WebDriver,
    private val scraperKlass: KClass<*>,
    private val retries: Int,
    private val model: LLM
) : IScraperWrapper {
    private val stableScraperPath = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/stable/scraper"
    private val initialScraperOutDir = "$stableScraperPath/out"
    private val latestSnapshotPath = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/latest"
    private val testRunPath = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/test"

    init {
        if (!scraperKlass.isSubclassOf(IScraper::class)) {
            throw IllegalArgumentException("Provided class must implement IScraper interface")
        }

        if (scraperKlass.simpleName == null || scraperKlass.simpleName!!.isEmpty()) {
            throw IllegalArgumentException("Scraper class name cannot be null or empty")
        }
    }

    /**
     * Performs web scraping using the provided scraper class.
     *
     * @return Boolean indicating whether the scraping was successful or if it needs correction.
     */
    override suspend fun scrape(): Boolean {
        val compilationResult = compileAndInstantiateScraper(scraperKlass, "$stableScraperPath/${scraperKlass.simpleName}.kt", "$stableScraperPath/${scraperKlass.simpleName}Test.kt")
        val scraperInstance = compilationResult.scraperInstance
        val testInstance = compilationResult.testInstance

        try {
            scraperInstance.scrape()
            return true
        } catch (e: Exception) {
            val seleniumExceptionTypes = setOf(
                NoSuchElementException::class,
                ElementNotInteractableException::class,
                StaleElementReferenceException::class,
                TimeoutException::class
            )

            if (seleniumExceptionTypes.any { it.isInstance(e) }) {
                println("Scraping exception occurred: ${e.message}. Trying to correct...")
                // return correctScraper()
            } else {
                snapshotService.takeSnapshotAsFile(driver)
                throw e
            }

            return false
        }

    }

    /**
     * Compiles the scraper's and test's code.
     *
     * @param scraperKlass The KClass of the scraper to be compiled.
     * @param scraperCodePath The path to the scraper code file.
     * @return CompiledScraperResult containing the instantiated scraper and test classes.
     */
    private fun compileAndInstantiateScraper(scraperKlass: KClass<*>, scraperCodePath: String, testFilePath: String ): CompiledScraperResult {
        val className = scraperKlass.simpleName!!
        val scraperFile = File(scraperCodePath)
        val scraperDir = scraperFile.parentFile
        val testFile = File(testFilePath)
        val testDir = testFile.parentFile
        val outDir = File(initialScraperOutDir)

        if (!scraperFile.exists() || !testFile.exists()) {
            throw IllegalArgumentException("Scraper or test file does not exist")
        }

        val compileResult = ScraperCompiler.attemptToCompileAndInstantiate(
            scraperName = className,
            scraperSourceDir = scraperDir,
            testSourceDir = testDir,
            outputDir = outDir,
            driver = driver,
            snapshotService = snapshotService
        )

        if (compileResult == null) {
            throw IllegalStateException("Failed to compile scraper: $className")
        }

        return compileResult
    }

}
package wrapper

import Configurations
import classes.data.Element
import classes.llm.LLM
import classes.scrapers.ScraperCorrection
import classes.service_model.Modification
import compiler.CompiledScraperResult
import compiler.ScraperCompiler
import domain.model.interfaces.IScraperWrapper
import html_fetcher.WebExtractor
import interfaces.IScraper
import modification_detection.IModificationService
import org.openqa.selenium.*
import persistence.PersistenceService
import snapshots.ISnapshotService
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class Scraper(
    private val modificationService: IModificationService,
    private val snapshotService: ISnapshotService,
    private val webExtractor: WebExtractor,
    private val persistenceService: PersistenceService,
    private val driver: WebDriver,
    private val scraperKlass: KClass<*>,
    private val retries: Int,
    private val model: LLM
) : IScraperWrapper {
    private val stableScraperBaseDir = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/stable/scraper"

    private val latestScraperBaseDir = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/latest/scraper"
    private val latestHtmlBaseDir = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/latest/html"

    private val initialScraperOutDir = "$stableScraperBaseDir/out"
    private val latestSnapshotPath = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/latest"

    private val testScraperBaseDir = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/test/scraper"
    private val testHtmlBaseDirPath = "${Configurations.snapshotBaseDir}${scraperKlass.simpleName}/test/html"

    private val scraperCorrectionHistory = listOf<ScraperCorrection>()

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
        val scraperPath = "$stableScraperBaseDir/${scraperKlass.simpleName}.kt"
        val compilationResult = compileAndInstantiateScraper(scraperKlass, scraperPath, "$stableScraperBaseDir/${scraperKlass.simpleName}Test.kt")
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

                val scraperCode = persistenceService.read(scraperPath)

                return correctScraper(scraperCode)
            } else {
                snapshotService.takeSnapshotAsFile(driver)
                throw e
            }

            return false
        }

    }

    private fun correctScraper(scraperCode: String): Boolean {
        /**
         * Call getMissingElements
         * Get alternative for each missing element
         * Get modifications
         * Call modify script with the oldScript and the modifications
         * Test the new scraper
         * - If passed
         *      - Persist scraper
         * - Otherwise, check if there was a partial fix
         *      - If partial fix
         *          - Save the correction result in the scraperCorrectionHistory list
         *          - Use test run scraper code and snapshots to try and correct the scraper again
         *      - Otherwise
         *          - Revert to the last correction result if there is any, otherwise, revert to the original code and retry
        */

        getHtmlAndScraperCode()

        getMissingElements(scraperCode)

        modificationService.modifyScriptChatHistory()
    }

    private fun getHtmlAndScraperCode(wasTherePartialFix: Boolean): Pair<String, String> {
        /**
         * - If not wasTherePartialFix
         *      - Get html code latest directory and scraper code from the stable directory
         * - Otherwise
         *      - Get html and scraper code from test directory
         */

        return if (!wasTherePartialFix) {
            val scraperCode = persistenceService.read("$stableScraperBaseDir/${scraperKlass.simpleName}.kt")
            val htmlCode = persistenceService.read("$latestHtmlBaseDir/index.html")
            Pair(scraperCode, htmlCode)
        } else {
            val scraperCode = persistenceService.read("$testScraperBaseDir/${scraperKlass.simpleName}.kt")
            val htmlCode = persistenceService.read("$testHtmlBaseDirPath/index.html")
            Pair(scraperCode, htmlCode)
        }
    }

    private fun getMissingElements(scraperCode: String, htmlSnapshot: String) {
        /**
         * Get selected elements information from scraper code using an LLM
         * Get relevant elements from htmlSnapshot
         * Return elements that are in the scraper code and not in the html relevant elements
         */
    }

    private fun getAlternative(missingElement: Element, htmlRelevantElements: List<Element>): Modification<Element> {
        /**
         * Use an LLM to find the best alternative given the missing elements and the new relevant elements from the html
         */
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
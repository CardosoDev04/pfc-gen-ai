package wrapper

import Configurations
import classes.data.Element
import classes.llm.LLM
import classes.scrapers.ScraperCorrection
import classes.service_model.Modification
import compiler.CompiledScraperResult
import compiler.ScraperCompiler
import domain.model.interfaces.IScraperWrapper
import domain.prompts.FEW_SHOT_SCRAPER_UPDATE_MESSAGES
import enums.ScraperCorrectionResult
import html_fetcher.WebExtractor
import interfaces.IScraper
import modification_detection.IModificationService
import org.openqa.selenium.*
import persistence.PersistenceService
import snapshots.ISnapshotService
import java.io.File
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class Scraper(
    private val modificationService: IModificationService,
    private val snapshotService: ISnapshotService,
    private val webExtractor: WebExtractor,
    private val persistenceService: PersistenceService,
    private val driver: WebDriver,
    private val scraperKlass: KClass<*>,
    private val maxRetries: Int,
    private val model: LLM
) : IScraperWrapper {
    private val stableBaseDir = "${Configurations.snapshotBaseDir}/${scraperKlass.simpleName}/stable"
    private val stableScraperBaseDir = "$stableBaseDir/scraper"

    private val latestBaseDir = "${Configurations.snapshotBaseDir}/${scraperKlass.simpleName}/latest"
    private val latestScraperBaseDir = "$latestBaseDir/scraper"
    private val latestHtmlBaseDir = "$latestBaseDir/html"

    private val testBaseDir = "${Configurations.snapshotBaseDir}/${scraperKlass.simpleName}/test"
    private val testScraperBaseDir = "$testBaseDir/scraper"
    private val testHtmlBaseDirPath = "$testBaseDir/html"

    private val initialScraperOutDir = "$stableScraperBaseDir/out"
    private val latestSnapshotPath = "${Configurations.snapshotBaseDir}/${scraperKlass.simpleName}/latest"

    private val scraperCorrectionHistory = mutableListOf<ScraperCorrection>()

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
     * @return The result of the scraping
     */
    override suspend fun scrape(): Any {
        val scraperInstance = compileAndCopyToLatest()

        try {
            return scraperInstance.scrape()
                .also { persistenceService.copyWholeDirectory(latestBaseDir, stableBaseDir) }
        } catch (e: Exception) {
            val seleniumExceptionTypes = setOf(
                NoSuchElementException::class,
                ElementNotInteractableException::class,
                StaleElementReferenceException::class,
                TimeoutException::class
            )

            if (seleniumExceptionTypes.any { it.isInstance(e) }) {
                println("Scraping exception occurred: ${e.message}. Trying to correct...")
                snapshotService.isFirstRun = false
                return attemptCorrectingScraper()
            }

            snapshotService.takeSnapshotAsFile(driver); TODO("Why?")
            throw e
        }
    }

    private fun compileAndCopyToLatest(): IScraper {
        val scraperPath = "$stableScraperBaseDir/${scraperKlass.simpleName}.kt"
        val scraperCode = persistenceService.read(scraperPath)
        persistenceService.write("$latestScraperBaseDir/${scraperKlass.simpleName}.kt", scraperCode)
        val compilationResult = compileAndInstantiateScraper(scraperKlass, scraperPath, "$stableScraperBaseDir/${scraperKlass.simpleName}Test.kt")
        return compilationResult.scraperInstance
    }

    private suspend fun attemptCorrectingScraper(): Any {
        var retries = 0

        while (retries < maxRetries) {
            val correctionResult = correctScraper()

            if (correctionResult is ScraperCorrectionResult.Success) {
                return correctionResult.correctedScraper.scrape()
                    .also { persistenceService.copyWholeDirectory(latestBaseDir, stableBaseDir) }
            } else if (correctionResult is ScraperCorrectionResult.Failure) {
                retries++
            }
        }

        throw Exception("Unable to correct scraper.")
    }

    private suspend fun correctScraper(): ScraperCorrectionResult {
        /**
         * Call getMissingElements
         * Get alternative for each missing element
         * Get modifications
         * Call modify script with the oldScript and the modifications
         * Compile and instantiate the new scraper
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

        val (scraperCode, htmlCode) = getHtmlAndScraperCode()
        val htmlElements = webExtractor.getRelevantHTMLElements(htmlCode)
        val missingElements = getMissingElements(scraperCode, htmlElements)
        val alternativeElements = missingElements.map { getAlternative(it, htmlElements) }

        val newScript = modificationService.modifyScriptChatHistory(scraperCode, alternativeElements, model.modelName, FEW_SHOT_SCRAPER_UPDATE_MESSAGES)

        val newScraperPath = "$testScraperBaseDir/${scraperKlass.simpleName}"
        persistenceService.write(newScraperPath, newScript)

        val scraperCompilationResult = compileAndInstantiateScraper(
            scraperKlass,
            newScraperPath,
            "$stableScraperBaseDir/${scraperKlass.simpleName}Test.kt"
        )

        val testResult = testScraper(scraperCompilationResult.scraperInstance)

        when (testResult) {
            is ScraperCorrectionResult.Failure -> { }
            is ScraperCorrectionResult.PartialFix -> {
                val stepsAchieved = testResult.stepsAchieved
                scraperCorrectionHistory.add(ScraperCorrection(newScript, stepsAchieved))
            }
            is ScraperCorrectionResult.Success -> {
                persistScraper(newScript)
            }
        }

        return testResult
    }

    private fun getHtmlAndScraperCode(): Pair<String, String> {
        /**
         * - If scraperCorrectionHistory get scraper and html code from the latest directory
         *      - Get html code latest directory and scraper code from the stable directory
         * - Otherwise
         *      - Get html from test directory and last correction result scraper code
         */
        if (scraperCorrectionHistory.isEmpty()) {
            val scraperCode = persistenceService.read("$stableScraperBaseDir/${scraperKlass.simpleName}.kt")
            val htmlCode = persistenceService.read("$latestHtmlBaseDir/index.html")
            return Pair(scraperCode, htmlCode)
        }

        val scraperCode = scraperCorrectionHistory.last().code
        val htmlCode = persistenceService.read("$testHtmlBaseDirPath/index.html")

        return Pair(scraperCode, htmlCode)
    }

    private suspend fun getMissingElements(scraperCode: String, htmlElements: List<Element>): List<Element> {
        /**
         * Get selected elements information from scraper code using an LLM
         * Get relevant elements from htmlSnapshot
         * Return elements that are in the scraper code and not in the html relevant elements
         */

        // TODO("Create system and prompt for getElementsFromScript")
        val scriptElements = modificationService.getElementsFromScript(scraperCode, "", "")

        return scriptElements.filter { !htmlElements.contains(it) }
    }

    private suspend fun getAlternative(missingElement: Element, htmlRelevantElements: List<Element>): Modification<Element> {
        /**
         * Use an LLM to find the best alternative given the missing elements and the new relevant elements from the html
         */
        return modificationService.getModification(missingElement, htmlRelevantElements)
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

    private fun testScraper(scraper: IScraper): ScraperCorrectionResult {
        println("Testing scraper...")

        getSetUpMethods(scraper::class)
            .forEach { it.invoke(scraper) }

        // Run all tests from the test class
        val testMethods = scraper::class.java.methods.filter {
            it.isAnnotationPresent(org.junit.jupiter.api.Test::class.java)
        }

        var failedTests = 0

        for (method in testMethods) {
            try {
                println("Running test: ${method.name}")
                method.invoke(scraper)
            } catch (e: Exception) {
                failedTests++
                println("Test failed: ${method.name}")
                println(e.cause?.message?.substringBefore("Build info:"))
                println(e.printStackTrace())
            }
        }

        getTearDownMethods(scraper::class)
            .forEach { it.invoke(scraper) }

        if (failedTests > 0) {
            val (lastRunSteps, currentRunSteps) = getStepsAchieved()

            if (currentRunSteps > lastRunSteps) {
                return ScraperCorrectionResult.PartialFix(currentRunSteps)
            }

            return ScraperCorrectionResult.Failure
        }

        return ScraperCorrectionResult.Success(scraper)
    }

    private fun getTearDownMethods(clazz: KClass<*>): List<Method> =
        clazz.java.methods.filter {
            it.isAnnotationPresent(org.junit.jupiter.api.AfterAll::class.java)
        }

    private fun getSetUpMethods(clazz: KClass<*>): List<Method> =
        clazz.java.methods.filter {
            it.isAnnotationPresent(org.junit.jupiter.api.BeforeAll::class.java)
        }

    private fun getStepsAchieved(): Pair<Int, Int> {
        /**
         * If there was no progress until now fixing the scraper
         *  - Get the steps from the original run and the current run's steps
         * Otherwise
         *  - Get the steps from the last scraper correction and the current run's steps
         */
        val testStepDir = persistenceService.findLastCreatedDirectory("$testBaseDir/steps")
            ?: throw IllegalStateException("Test steps directory not found")

        if (scraperCorrectionHistory.isEmpty()) {
            val latestStepDir = persistenceService.findLastCreatedDirectory("$latestBaseDir/steps")
                ?: throw IllegalStateException("Latest steps directory not found")

            return Pair(latestStepDir.name.toInt(), testStepDir.name.toInt())
        }

        return Pair(scraperCorrectionHistory.last().stepsAchieved, testStepDir.name.toInt())
    }

    private fun persistScraper(newScript: String) {
        persistenceService.write("$stableScraperBaseDir/${scraperKlass.simpleName}.kt", newScript)
    }
}
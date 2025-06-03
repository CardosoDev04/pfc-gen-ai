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
import domain.prompts.GET_MISSING_ELEMENTS_MESSAGES
import domain.prompts.GET_MISSING_ELEMENTS_SYSTEM_PROMPT
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
import Utils.replaceFirstLine

class Scraper(
    private val scraper: IScraper,
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
    // private val latestHtmlBaseDir = "$latestBaseDir/html"

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
        copyToLatest()

        try {
            return scraper.scrape()
                .also {
                    persistenceService.copyWholeDirectory(latestBaseDir, stableBaseDir)
                    persistenceService.deleteSubDirectories(latestBaseDir)
                }
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

            throw e
        }
    }

    private fun copyToLatest() {
        val scraperPath = "$stableScraperBaseDir/${scraperKlass.simpleName}.kt"
        val scraperCode = persistenceService.read(scraperPath)
        persistenceService.write("$latestScraperBaseDir/${scraperKlass.simpleName}.kt", scraperCode)
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
        val notPresent = getMissingElements(scraperCode, htmlElements)

//        val newScript = modificationService.modifyScriptChatHistoryV2(
//            scraperCode,
//            notPresent,
//            model.modelName,
//            FEW_SHOT_SCRAPER_UPDATE_MESSAGES
//        )
        val newScript = """
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
                    try {
                        val identifier = StepTracker.initializeRun()
            
                        val webDriverWait = WebDriverWait(driver, Duration.ofSeconds(5))
                        driver.get("http://localhost:5173/")
            
                        webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("search-btn")))
                        StepTracker.incrementStep(identifier)
            
                        snapshotService.takeSnapshotAsFile(driver)
            
                        val optionElements = webDriverWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("item-title")))
                        StepTracker.incrementStep(identifier)
            
                        snapshotService.takeSnapshotAsFile(driver)
            
                        val results = optionElements.map { BookingOption(it.text) }
            
                        return results
                    } catch (e: Exception) {
                        snapshotService.takeSnapshotAsFile(driver)
                        throw e
                    }
                }
            
                override fun getScraperData(): IScraperData = DemoScraperDataBundle(
                    path = Configurations.snapshotBaseDir + "stable/" + this::class.simpleName + ".kt",
                    compiledClass = this
                )
            }
        """.trimIndent()

        val newScraperPath = "$testScraperBaseDir/${scraperKlass.simpleName}.kt"
        val newTestPath = "$testScraperBaseDir/${scraperKlass.simpleName}Test.kt"

        persistenceService.write(newScraperPath, newScript)

        val testFileContent = persistenceService.read("$stableBaseDir/scraper/${scraperKlass.simpleName}Test.kt")
        persistenceService.write(newTestPath, testFileContent)

        val scraperCompilationResult = compileAndInstantiateScraper(
            scraperKlass,
            newScraperPath,
            newTestPath
        )

        val testResult = testScraper(scraperCompilationResult)

        when (testResult) {
            is ScraperCorrectionResult.Failure -> {}
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

        /*
        val latestStepDir = persistenceService.findLastCreatedDirectory("$latestBaseDir/steps")
                ?: throw IllegalStateException("Latest steps directory not found")

            return Pair(latestStepDir.name.toInt(), testStepDir.name.toInt())
         */
        val lastAchievedStep = persistenceService.findLastCreatedDirectory("$latestBaseDir/steps")?.name?.toInt()
            ?: throw IllegalStateException("Latest steps directory not found")

        if (scraperCorrectionHistory.isEmpty()) {
            val scraperCode = persistenceService.read("$stableScraperBaseDir/${scraperKlass.simpleName}.kt")
            val htmlCode = persistenceService.read("$latestBaseDir/steps/$lastAchievedStep/html/index.html")
            return Pair(scraperCode, htmlCode)
        }

        val scraperCode = scraperCorrectionHistory.last().code
        val htmlCode = persistenceService.read("$testHtmlBaseDirPath/index.html")

        return Pair(scraperCode, htmlCode)
    }

    private suspend fun getMissingElements(scraperCode: String, htmlElements: List<Element>): List<Element> {
        return modificationService.getMissingElementsFromScript(
            scraperCode,
            htmlElements,
            GET_MISSING_ELEMENTS_SYSTEM_PROMPT,
            GET_MISSING_ELEMENTS_MESSAGES
        )
    }

    private suspend fun getAlternative(
        missingElement: Element,
        htmlRelevantElements: List<Element>
    ): Modification<Element> {
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
    private fun compileAndInstantiateScraper(
        scraperKlass: KClass<*>,
        scraperCodePath: String,
        testFilePath: String
    ): CompiledScraperResult {
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

    private fun testScraper(scraperResult: CompiledScraperResult): ScraperCorrectionResult {
        println("Testing scraper...")

        val testInstance = scraperResult.testInstance
        val testClass = testInstance::class

        getSetUpMethods(testClass).forEach { it.invoke(testInstance) }

        val testMethods = testClass.java.methods.filter {
            it.isAnnotationPresent(org.junit.jupiter.api.Test::class.java)
        }

        var failedTests = 0

        for (method in testMethods) {
            try {
                println("Running test: ${method.name}")
                method.invoke(testInstance)
            } catch (e: Exception) {
                failedTests++
                println("Test failed: ${method.name}")
                println(e.cause?.message?.substringBefore("Build info:"))
                println(e.printStackTrace())
            }
        }

        getTearDownMethods(testClass).forEach { it.invoke(testInstance) }

        if (failedTests > 0) {
            val (lastRunSteps, currentRunSteps) = getStepsAchieved()

            if (currentRunSteps > lastRunSteps) {
                return ScraperCorrectionResult.PartialFix(currentRunSteps)
            }

            return ScraperCorrectionResult.Failure
        }

        return ScraperCorrectionResult.Success(scraperResult.scraperInstance)
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
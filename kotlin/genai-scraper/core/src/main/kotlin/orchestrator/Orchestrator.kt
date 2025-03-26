package orchestrator

import classes.llm.LLM
import classes.llm.Model
import classes.scrapers.DemoScraperBundle
import com.cardoso.common.buildChromeDriver
import core.ExecutionTracker
import core.TestReportService
import demo.DemoScraper
import domain.interfaces.ITestReportService
import domain.model.interfaces.IOrchestrator
import interfaces.IScraper
import html_fetcher.WebExtractor
import kotlinx.coroutines.runBlocking
import modification_detection.IModificationDetectionService
import modification_detection.ModificationDetectionService
import okhttp3.OkHttpClient
import ollama.OllamaClient
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import org.openqa.selenium.ElementNotInteractableException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import persistence.PersistenceService
import persistence.implementations.FilePersistenceService
import snapshots.ISnapshotService
import snapshots.SnapshotService
import utils.TimeStampService
import java.io.File
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.TimeUnit
import javax.tools.ToolProvider

fun findLastCreatedDirectory(directoryPath: String): File? {
    val directory = File(directoryPath)
    if (!directory.exists() || !directory.isDirectory) {
        throw IllegalArgumentException("The provided path is not a valid directory")
    }

    return directory.listFiles { file -> file.isDirectory }?.maxByOrNull { it.lastModified() }
}

class Orchestrator(
    private val modificationDetectionService: IModificationDetectionService,
    private val snapshotService: ISnapshotService,
    private val testReportService: ITestReportService,
    private val webExtractor: WebExtractor,
    private val filePersistenceService: PersistenceService,
    private val model: Model,
    private val driver: WebDriver
) : IOrchestrator {

    /**
     * Corrects the scraper by identifying modifications and recompiling the scraper.
     *
     * @param oldScraper The old scraper instance.
     * @param retries The number of retries allowed if the scraper fails tests.
     */
    override suspend fun correctScraper(oldScraper: IScraper, stepName: String, retries: Int) {
        val latestSnapshot = snapshotService.getSnapshot("/Users/joaocardoso/Documents/Faculdade/PFC/pfc-gen-ai/kotlin/genai-scraper/core/src/main/kotlin/snapshots/demo_website/get_options/latest/$stepName/html/source.html")
        val latestStableSnapshot = snapshotService.getSnapshot("/Users/joaocardoso/Documents/Faculdade/PFC/pfc-gen-ai/kotlin/genai-scraper/core/src/main/kotlin/snapshots/demo_website/get_options/latest_stable/$stepName/html/source.html")
        val latestSnapshotHtml = latestSnapshot.html.readText()
        val latestStableSnapshotHtml = latestStableSnapshot.html.readText()

        val modifiedElements =
            modificationDetectionService.getMissingElements(latestStableSnapshotHtml, latestSnapshotHtml)
        val newElements = webExtractor.getInteractiveElementsHTML(latestSnapshotHtml)
        val modifications = modifiedElements.map { modificationDetectionService.getModification(it, newElements) }

        val newScript = modificationDetectionService.modifyScript(oldScraper.code, modifications)

        filePersistenceService.write("/Users/joaocardoso/Documents/Faculdade/PFC/pfc-gen-ai/kotlin/genai-scraper/core/src/main/kotlin/working/to_test/toTest.kt", newScript)

        val newScraper = compileAndInstantiateNewScraper("kotlin/working/to_test/toTest.kt")

        try {
            testScraper(newScraper)
        } catch (e: Exception) {
            println("Scraper did not pass automated tests. Retries left: $retries")
            if (retries - 1 > 0) correctScraper(oldScraper, stepName,retries - 1)
        }
    }

    /**
     * Compiles and instantiates a new scraper from the given Kotlin file path.
     *
     * @param scraperCodePath The path to the Kotlin file containing the scraper code.
     * @return The instantiated IScraper object.
     * @throws RuntimeException If the compilation of the Kotlin file fails.
     */
    override suspend fun compileAndInstantiateNewScraper(scraperCodePath: String): IScraper {
        val file = File(scraperCodePath)
        val outputDir = file.parentFile

        // Compile the Kotlin file using kotlinc
        val process = ProcessBuilder(
            "kotlinc", file.path, "-d", outputDir.path
        ).inheritIO().start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            file.delete()
            throw RuntimeException("Failed to compile Kotlin file: $scraperCodePath")
        }

        val classLoader = URLClassLoader.newInstance(arrayOf(outputDir.toURI().toURL()))
        val className = file.nameWithoutExtension.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val clazz = classLoader.loadClass(className)

        return clazz.getDeclaredConstructor().newInstance() as IScraper
    }

    /**
     * Runs the given scraper.
     *
     * @param scraper The scraper instance to run.
     */
    override suspend fun runDemoScraper(scraper: DemoScraperBundle) {
        try {
            scraper.compiledClass.getBookingOptions()
        } catch (e: Exception) {

            val lastCreated = findLastCreatedDirectory("/Users/joaocardoso/Documents/Faculdade/PFC/pfc-gen-ai/kotlin/genai-scraper/core/src/main/kotlin/snapshots/demo_website/get_options/latest")
                ?: throw IllegalStateException("Could not find step directory.")
            val stepName = lastCreated.name

            val exceptionsToCheck = listOf(
                NoSuchElementException::class,
                StaleElementReferenceException::class,
                ElementNotInteractableException::class,
                TimeoutException::class
            )

            for (exception in exceptionsToCheck) {
                if (exception.isInstance(e)) {
                    println("Caught exception: ${e::class.simpleName}")
                    correctScraper(scraper, stepName, 3)
                }
            }

            throw e
        }
    }

    /**
     * Tests the given scraper.
     *
     * @param scraper The scraper instance to test.
     */
    override suspend fun testScraper(scraper: IScraper) {
        val summaryGeneratingListener = SummaryGeneratingListener()

        val request: LauncherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(scraper::class.java))
            .build()

        val launcher = LauncherFactory.create()
        launcher.registerTestExecutionListeners(summaryGeneratingListener)
        launcher.execute(request)

        val summary: TestExecutionSummary = summaryGeneratingListener.summary
        if (summary.totalFailureCount > 0) {
            throw RuntimeException("Scraper tests failed")
        }
    }
}

fun main() {
    val httpCli = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    val llmCli = OllamaClient(httpCli)
    val mds = ModificationDetectionService(llmCli)

    val snapshotServ = SnapshotService()
    val timeStampService = TimeStampService()
    val executionTracker = ExecutionTracker(timeStampService)
    val testReportServ = TestReportService(executionTracker, timeStampService)
    val webExtractor = WebExtractor()
    val fps = FilePersistenceService()

    val driver = buildChromeDriver()

    val orchestrator = Orchestrator(
        modificationDetectionService = mds,
        snapshotService = snapshotServ,
        testReportService = testReportServ,
        webExtractor = webExtractor,
        filePersistenceService = fps,
        model = Model("Mistral_7B", "mistral:7b", 7L, ""),
        driver
    )

    val demoScraper = DemoScraper(driver, snapshotServ)
    val demoScraperBundle = DemoScraperBundle(fps.read("/Users/joaocardoso/Documents/Faculdade/PFC/pfc-gen-ai/kotlin/genai-scraper/scrapers/src/main/kotlin/demo/DemoScraper.kt"), demoScraper)

    runBlocking {
        orchestrator.runDemoScraper(demoScraperBundle)
    }
}
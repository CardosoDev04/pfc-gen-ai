package orchestrator

import classes.llm.LLM
import classes.llm.Model
import classes.scrapers.DemoScraperDataBundle
import com.cardoso.common.buildChromeDriver
import core.ExecutionTracker
import core.TestReportService
import domain.interfaces.ITestReportService
import domain.model.interfaces.IOrchestrator
import domain.prompts.CODE_LLAMA_SCRAPER_UPDATE_PROMPT_SYSTEM
import domain.prompts.GET_MODIFICATION_PROMPT
import domain.prompts.CODE_LLAMA_SCRAPER_UPDATE_PROMPT_USER
import interfaces.IScraperData
import html_fetcher.WebExtractor
import interfaces.IScraper
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
import scrapers.DemoScraper
import snapshots.ISnapshotService
import snapshots.SnapshotService
import utils.TimeStampService
import java.io.File
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.TimeUnit

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
    override suspend fun correctScraper(oldScraper: IScraperData, stepName: String, retries: Int) {
        val latestSnapshot = snapshotService.getSnapshot(Configurations.snapshotBaseDir + "${oldScraper.name}/latest/$stepName/html/source.html")
        val latestStableSnapshot = snapshotService.getSnapshot(Configurations.snapshotBaseDir + "${oldScraper.name}/latest_stable/$stepName/html/source.html")
        val latestSnapshotHtml = latestSnapshot.html.readText()
        val latestStableSnapshotHtml = latestStableSnapshot.html.readText()

        val modifiedElements = modificationDetectionService.getMissingElements(latestStableSnapshotHtml, latestSnapshotHtml)
        val newElements = webExtractor.getInteractiveElementsHTML(latestSnapshotHtml)
        val modifications = modifiedElements.map { modificationDetectionService.getModification(it, newElements) }

        val newScript = when(modelName) {
            LLM.Mistral7B.modelName -> modificationDetectionService.modifyMistralScript(oldScraper.code, modifications, modelName, prompt)
            LLM.CodeLlama7B.modelName -> modificationDetectionService.modifyCodeGenerationLLMScript(oldScraper.code, modifications, LLM.CodeLlama7B.modelName, CODE_LLAMA_SCRAPER_UPDATE_PROMPT_SYSTEM, prompt )
            LLM.DeepSeekCoder1Point3B.modelName -> modificationDetectionService.modifyCodeGenerationLLMScript(oldScraper.code, modifications, LLM.CodeLlama7B.modelName, CODE_LLAMA_SCRAPER_UPDATE_PROMPT_SYSTEM, prompt )
            LLM.Gemma3_1B.modelName -> modificationDetectionService.modifyCodeGenerationLLMScript(oldScraper.code, modifications, LLM.CodeLlama7B.modelName, CODE_LLAMA_SCRAPER_UPDATE_PROMPT_SYSTEM, prompt )
            else -> throw Exception("Unrecognized model name.")
        }

        filePersistenceService.write(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", newScript)

        val newScraper = compileAndInstantiateNewScraper(Configurations.scrapersBaseDir + "${oldScraper.name}.kt")

        try {
            testScraper(newScraper)
        } catch (e: Exception) {
            println("Scraper did not pass automated tests. Retries left: $retries")
            if (retries - 1 > 0) correctScraper(oldScraper, stepName,retries - 1)
            throw Exception("Could not correct scraper: $e")
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

        // Get the current classpath
        val classpath = System.getProperty("java.class.path")

        // Compile the Kotlin file using kotlinc
        val process = ProcessBuilder(
            "kotlinc", file.path, "-d", outputDir.path , "-classpath", classpath
        ).inheritIO().start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            file.delete()
            throw RuntimeException("Failed to compile Kotlin file: $scraperCodePath")
        }

        val newClassLoader = URLClassLoader.newInstance(arrayOf(outputDir.toURI().toURL()), this::class.java.classLoader)
        val className = file.nameWithoutExtension.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val clazz = newClassLoader.loadClass("scrapers.$className")

        return clazz.getDeclaredConstructor(WebDriver::class.java, ISnapshotService::class.java).newInstance(driver, snapshotService) as IScraper
    }

    /**
     * Runs the given scraper.
     *
     * @param scraper The scraper instance to run.
     */
    override suspend fun runScraper(scraper: IScraperData, snapshotsPath: String) {
        try {
            scraper.compiledClass.scrape()
        } catch (e: Exception) {

            val lastCreated = findLastCreatedDirectory(snapshotsPath)
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
                    return
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

    companion object {
        var prompt = CODE_LLAMA_SCRAPER_UPDATE_PROMPT_USER
        val modelName = LLM.Gemma3_1B.modelName
    }
}

fun main() {
    val httpCli = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    val llmCli = OllamaClient(httpCli)

    val mds = ModificationDetectionService(
        llmClient = llmCli,
        getModificationModel = LLM.Mistral7B.modelName,
        getModificationSystemPrompt = GET_MODIFICATION_PROMPT
    )

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
    val demoScraperBundle = DemoScraperDataBundle(Configurations.scrapersBaseDir + "DemoScraper.kt", demoScraper)

    runBlocking {
        orchestrator.runScraper(demoScraperBundle, Configurations.snapshotBaseDir + "DemoScraper/latest")
    }
}
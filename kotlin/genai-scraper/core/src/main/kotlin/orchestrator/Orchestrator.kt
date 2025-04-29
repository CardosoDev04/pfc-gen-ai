package orchestrator

import classes.data.Element
import classes.llm.LLM
import classes.scrapers.DemoScraperDataBundle
import classes.service_model.Modification
import com.cardoso.common.buildChromeDriver
import domain.model.interfaces.IOrchestrator
import domain.prompts.*
import interfaces.IScraperData
import html_fetcher.WebExtractor
import interfaces.IScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    private val webExtractor: WebExtractor,
    private val persistenceService: PersistenceService,
    private val driver: WebDriver
) : IOrchestrator {

    /**
     * Corrects the scraper by identifying modifications and recompiling the scraper.
     *
     * @param oldScraper The old scraper instance.
     * @param retries The number of retries allowed if the scraper fails tests.
     */
    override suspend fun correctScraper(oldScraper: IScraperData, modifications: List<Modification<Element>>, retries: Int): Boolean {
        var currRetries = retries
        var success = false

        while (!success && currRetries > 0) {
            println("Attempt ${retries - currRetries + 1}")

            saveOldScript(oldScraper)
            success = attemptCorrectingScraper(oldScraper, modifications)
            currRetries--

            if (!success) {
                persistenceService.copyAndDeleteFile(Configurations.tempBaseDir + "${oldScraper.name}.kt", oldScraper.path)
            }
        }

        return success
    }

    private fun saveOldScript(oldScraper: IScraperData) {
        persistenceService.write(Configurations.tempBaseDir + "${oldScraper.name}.kt", oldScraper.code)
    }

    private suspend fun attemptCorrectingScraper(oldScraper: IScraperData, modifications: List<Modification<Element>>): Boolean {
        // Get the fixed script from the LLM
        val newScript = when(modelName) {
            LLM.Mistral7B.modelName -> modificationDetectionService.modifyScriptChatHistory(oldScraper.code, modifications, LLM.Mistral7B.modelName, FEW_SHOT_SCRAPER_UPDATE_MESSAGES)
            LLM.CodeLlama7B.modelName -> modificationDetectionService.modifyScriptChatHistory(oldScraper.code, modifications, LLM.CodeLlama7B.modelName, FEW_SHOT_SCRAPER_UPDATE_MESSAGES)
            LLM.DeepSeekCoder1Point3B.modelName -> modificationDetectionService.modifyScriptChatHistory(oldScraper.code, modifications, LLM.DeepSeekCoder1Point3B.modelName, FEW_SHOT_SCRAPER_UPDATE_MESSAGES)
            LLM.Gemma3_1B.modelName -> modificationDetectionService.modifyScriptChatHistory(oldScraper.code, modifications, LLM.Gemma3_1B.modelName, FEW_SHOT_SCRAPER_UPDATE_MESSAGES)
            else -> throw Exception("Unrecognized model name.")
        }

        // Overwrite scraper's source code
        persistenceService.write(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", newScript)

        val newScraper = attemptToCompileAndInstantiateNewScraper(Configurations.scrapersBaseDir + "${oldScraper.name}.kt")

        if (newScraper == null) {
            println("Compilation of the new scraper failed")
            val oldScript = persistenceService.read(Configurations.tempBaseDir + "${oldScraper.name}.kt")
            persistenceService.write(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", oldScript)

            return false
        }

        try {
            testScraper(newScraper)
        } catch (e: Exception) {
            println("Scraper did not pass automated tests.")
            return false
        }

        println("Scraper tests were successful!")
        return true
    }

    /**
     * Compiles and instantiates a new scraper from the given Kotlin file path.
     *
     * @param scraperCodePath The path to the Kotlin file containing the scraper code.
     * @return The instantiated IScraper object or null in case of compilation failure.
     */
    override suspend fun attemptToCompileAndInstantiateNewScraper(scraperCodePath: String): IScraper? = withContext(Dispatchers.IO){
        try {
            val file = File(scraperCodePath)
            val outputDir = file.parentFile

            // Get the current classpath
            val classpath = System.getProperty("java.class.path")

            // Compile the Kotlin file using kotlinc
            val process = ProcessBuilder("kotlinc", file.path, "-d", outputDir.path , "-classpath", classpath)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                file.delete()
                return@withContext null
            }

            val newClassLoader = URLClassLoader.newInstance(arrayOf(outputDir.toURI().toURL()), this::class.java.classLoader)
            val className = file.nameWithoutExtension.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val clazz = newClassLoader.loadClass("scrapers.$className")

            return@withContext clazz.getDeclaredConstructor(WebDriver::class.java, ISnapshotService::class.java).newInstance(driver, snapshotService) as IScraper

        } catch (e: RuntimeException) {
            return@withContext null
        }
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
                    val modifications = getModifications(scraper, stepName)
                    val wasSuccessful = correctScraper(scraper, modifications)

                    if (!wasSuccessful) {
                        println("Scraper correction failed.")
                    }

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
    override suspend fun testScraper(scraper: IScraper): Boolean {
        println("Testing scraper...")
        val summaryGeneratingListener = SummaryGeneratingListener()

        val request: LauncherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(scraper::class.java))
            .build()

        val launcher = LauncherFactory.create()
        launcher.registerTestExecutionListeners(summaryGeneratingListener)
        launcher.execute(request)

        val summary: TestExecutionSummary = summaryGeneratingListener.summary

        return summary.totalFailureCount.toInt() == 0
    }

    private suspend fun getModifications(oldScraper: IScraperData, stepName: String): List<Modification<Element>> {
        val latestSnapshot = snapshotService.getSnapshot(Configurations.snapshotBaseDir + "${oldScraper.name}/latest/$stepName/html/source.html")
        val latestStableSnapshot = snapshotService.getSnapshot(Configurations.snapshotBaseDir + "${oldScraper.name}/latest_stable/$stepName/html/source.html")
        val latestSnapshotHtml = latestSnapshot.html.readText()
        val latestStableSnapshotHtml = latestStableSnapshot.html.readText()

        val modifiedElements = modificationDetectionService.getMissingElements(latestStableSnapshotHtml, latestSnapshotHtml)
        val newElements = webExtractor.getInteractiveElementsHTML(latestSnapshotHtml)
        return modifiedElements.map { modificationDetectionService.getModification(it, newElements) }
    }

    companion object {
        var prompt = FEW_SHOT_SCRAPER_UPDATE_MESSAGES
        val modelName = LLM.Llama8B.modelName
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
    val webExtractor = WebExtractor()
    val fps = FilePersistenceService()

    val driver = buildChromeDriver()

    val orchestrator = Orchestrator(
        modificationDetectionService = mds,
        snapshotService = snapshotServ,
        webExtractor = webExtractor,
        persistenceService = fps,
        driver
    )

    val demoScraper = DemoScraper(driver, snapshotServ)
    val demoScraperBundle = DemoScraperDataBundle(Configurations.scrapersBaseDir + "DemoScraper.kt", demoScraper)

    runBlocking {
        orchestrator.runScraper(demoScraperBundle, Configurations.snapshotBaseDir + "DemoScraper/latest")
    }
}
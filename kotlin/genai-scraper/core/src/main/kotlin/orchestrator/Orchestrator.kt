package orchestrator

import Configurations
import classes.data.Element
import classes.llm.LLM
import classes.service_model.Modification
import domain.model.classes.data.CompiledScraperResult
import domain.model.interfaces.IOrchestrator
import domain.prompts.FEW_SHOT_SCRAPER_UPDATE_MESSAGES
import html_fetcher.WebExtractor
import interfaces.IScraper
import interfaces.IScraperData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import library.wrappers.GenericScraper
import modification_detection.IModificationDetectionService
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import org.openqa.selenium.*
import org.openqa.selenium.NoSuchElementException
import persistence.PersistenceService
import snapshots.ISnapshotService
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.reflect.KClass

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
    private val driver: WebDriver,
    private val scraperTestKlass: KClass<*>
) : IOrchestrator {

    /**
     * Corrects the scraper by identifying modifications and recompiling the scraper.
     *
     * @param oldScraper The old scraper instance.
     * @param retries The number of retries allowed if the scraper fails tests.
     */
    override suspend fun correctScraper(
        oldScraper: IScraperData,
        modifications: List<Modification<Element>>,
        wrapper: GenericScraper,
        retries: Int
    ): Boolean {
        return attemptCorrectingScraper(oldScraper, modifications, wrapper)
    }

    private fun saveOldScript(oldScraper: IScraperData) {
        persistenceService.write(Configurations.versioningBaseDir + "${oldScraper.name}.kt", oldScraper.code)
    }

    private suspend fun attemptCorrectingScraper(
        oldScraper: IScraperData,
        modifications: List<Modification<Element>>,
        wrapper: GenericScraper
    ): Boolean {
        // Get the fixed script from the LLM
        val newScript = when (modelName) {
            LLM.Mistral7B.modelName -> modificationDetectionService.modifyScriptChatHistory(
                oldScraper.code,
                modifications,
                LLM.Mistral7B.modelName,
                FEW_SHOT_SCRAPER_UPDATE_MESSAGES
            )

            LLM.CodeLlama7B.modelName -> modificationDetectionService.modifyScriptChatHistory(
                oldScraper.code,
                modifications,
                LLM.CodeLlama7B.modelName,
                FEW_SHOT_SCRAPER_UPDATE_MESSAGES
            )

            LLM.DeepSeekCoder1Point3B.modelName -> modificationDetectionService.modifyScriptChatHistory(
                oldScraper.code,
                modifications,
                LLM.DeepSeekCoder1Point3B.modelName,
                FEW_SHOT_SCRAPER_UPDATE_MESSAGES
            )

            LLM.Gemma3_1B.modelName -> modificationDetectionService.modifyScriptChatHistory(
                oldScraper.code,
                modifications,
                LLM.Gemma3_1B.modelName,
                FEW_SHOT_SCRAPER_UPDATE_MESSAGES
            )

            else -> throw Exception("Unrecognized model name.")
        }

        // Overwrite scraper's source code
        persistenceService.write(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", newScript)

        val newScraperResult =
            attemptToCompileAndInstantiateNewScraper(Configurations.scrapersBaseDir + "${oldScraper.name}.kt")

        if (newScraperResult == null) {
            println("Compilation of the new scraper failed")
            val oldScript = persistenceService.read(Configurations.versioningBaseDir + "${oldScraper.name}.kt")
            persistenceService.write(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", oldScript)

            return false
        }

        val oldInstance = wrapper.getScraperInstance()
        val oldClassLoader = wrapper.getClassLoader()

        // Sets inner references for the wrapper object as the new scraper
        wrapper.setScraperInstance(newScraperResult.scraper, newScraperResult.classLoader)


        try {
            val success = testScraper(wrapper.getScraperInstance())
            if (!success) {
                // Revert changes
                wrapper.setScraperInstance(oldInstance, oldClassLoader)
                throw IllegalStateException("Scraper tests failed.")
            }
        } catch (e: Exception) {
            println("${e.message}")
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
    override suspend fun attemptToCompileAndInstantiateNewScraper(scraperCodePath: String): CompiledScraperResult? =
        withContext(Dispatchers.IO) {
            try {
                val originalFile = File(scraperCodePath)

                // Clear the versioning folder
                val versioningBaseDir = Paths.get(Configurations.versioningBaseDir)
                if (Files.exists(versioningBaseDir)) {
                    Files.walk(versioningBaseDir)
                        .sorted(Comparator.reverseOrder())
                        .map { it.toFile() }
                        .forEach { it.delete() }
                }

                // Create a new versioning folder
                val tempFolder = File("${Configurations.versioningBaseDir}/compiled_${System.currentTimeMillis()}")
                tempFolder.mkdirs()

                // Copy the Kotlin file to the versioning folder
                val tempKtFile = File(tempFolder, originalFile.name)
                originalFile.copyTo(tempKtFile)

                // Compile to the versioning folder
                val classpath = System.getProperty("java.class.path")
                val process = ProcessBuilder("kotlinc", tempKtFile.path, "-d", tempFolder.path, "-classpath", classpath)
                    .redirectErrorStream(true)
                    .start()

                val compilerOutput = process.inputStream.bufferedReader().readText()
                println("Compiler output:\n$compilerOutput")

                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    println("Compilation failed for ${tempKtFile.name}")
                    return@withContext null
                }

                val newClassLoader =
                    URLClassLoader.newInstance(arrayOf(tempFolder.toURI().toURL()), this::class.java.classLoader)
                val className = tempKtFile.nameWithoutExtension.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                val clazz = newClassLoader.loadClass("scrapers.$className")

                return@withContext CompiledScraperResult(clazz.getDeclaredConstructor(WebDriver::class.java, ISnapshotService::class.java)
                    .newInstance(driver, snapshotService) as IScraper, newClassLoader)

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }


    /**
     * Runs the given scraper.
     *
     * @param wrapper The wrapper for the scraper.
     * @param snapshotsPath The path to the snapshots' directory.
     */
    override suspend fun runScraper(wrapper: GenericScraper, snapshotsPath: String): Boolean {
        try {
            wrapper.getScraperInstance().scrape()
            return true
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
                    val modifications = getModifications(wrapper.getScraperData(), stepName)
                    val wasSuccessful = correctScraper(wrapper.getScraperData(), modifications, wrapper)

                    if (!wasSuccessful) {
                        println("Scraper correction failed.")
                    }

                    return wasSuccessful
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

        // Ensure the test class is of the expected type
        val testInstance = scraperTestKlass.objectInstance ?: scraperTestKlass.java.getDeclaredConstructor().newInstance()

        // Use reflection to find and call the setScraper method
        val setScraperMethod = scraperTestKlass.java.methods.find { it.name == "setScraper" }
        setScraperMethod?.invoke(testInstance, scraper)

        val summaryGeneratingListener = SummaryGeneratingListener()

        val request: LauncherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(scraperTestKlass.java))
            .build()

        val launcher = LauncherFactory.create()
        launcher.registerTestExecutionListeners(summaryGeneratingListener)
        launcher.execute(request)

        val summary: TestExecutionSummary = summaryGeneratingListener.summary

        println("Total tests discovered: ${summary.testsFoundCount}")

        summary.failures.forEach { failure ->
            println("Test failed: ${failure.testIdentifier.displayName}")
            failure.exception.printStackTrace()
        }

        return summary.totalFailureCount.toInt() == 0
    }

    private suspend fun getModifications(oldScraper: IScraperData, stepName: String): List<Modification<Element>> {
        val latestSnapshot =
            snapshotService.getSnapshot(Configurations.snapshotBaseDir + "${oldScraper.name}/latest/$stepName/html/source.html")
        val latestStableSnapshot =
            snapshotService.getSnapshot(Configurations.snapshotBaseDir + "${oldScraper.name}/latest_stable/$stepName/html/source.html")
        val latestSnapshotHtml = latestSnapshot.html.readText()
        val latestStableSnapshotHtml = latestStableSnapshot.html.readText()

        val previousElements = webExtractor.getRelevantHTMLElements(latestStableSnapshotHtml)
        val newElements = webExtractor.getRelevantHTMLElements(latestSnapshotHtml)
        val modifiedElements = modificationDetectionService.getMissingElements(previousElements, newElements)

        return modifiedElements.map { modificationDetectionService.getModification(it, newElements) }
    }

    companion object {
        var prompt = FEW_SHOT_SCRAPER_UPDATE_MESSAGES
        val modelName = LLM.Mistral7B.modelName
    }
}
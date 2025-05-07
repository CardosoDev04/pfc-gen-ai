package wrapper

import Configurations
import classes.data.Element
import classes.llm.LLM
import classes.service_model.Modification
import compiler.ScraperCompiler
import domain.model.classes.data.CompiledScraperResult
import domain.model.interfaces.IScraperWrapper
import domain.prompts.FEW_SHOT_SCRAPER_UPDATE_MESSAGES
import html_fetcher.WebExtractor
import interfaces.IScraperData
import modification_detection.IModificationDetectionService
import okio.Closeable
import org.openqa.selenium.*
import org.openqa.selenium.NoSuchElementException
import persistence.PersistenceService
import snapshots.ISnapshotService
import java.io.File
import java.lang.management.ClassLoadingMXBean
import java.lang.management.ManagementFactory
import java.lang.reflect.Method
import kotlin.reflect.KClass

fun findLastCreatedDirectory(directoryPath: String): File? {
    val directory = File(directoryPath)
    if (!directory.exists() || !directory.isDirectory) {
        throw IllegalArgumentException("The provided path is not a valid directory")
    }

    return directory.listFiles { file -> file.isDirectory }?.maxByOrNull { it.lastModified() }
}

class Scraper(
    private val modificationDetectionService: IModificationDetectionService,
    private val snapshotService: ISnapshotService,
    private val webExtractor: WebExtractor,
    private val persistenceService: PersistenceService,
    private val driver: WebDriver,
    private val scraperTestClassName: String,
    private var backupScraper: CompiledScraperResult?,
    private var currentScraper: CompiledScraperResult?,
    private val retries: Int,
    private val model: LLM
) : IScraperWrapper, Closeable {

    private fun closeBackup() {
        backupScraper?.classLoader?.close()
        backupScraper = null
        System.gc()
    }

    private fun closeCurrent() {
        currentScraper?.classLoader?.close()
        currentScraper = null
        System.gc()
    }

    private suspend fun correctScraper(
        oldScraper: IScraperData,
        modifications: List<Modification<Element>>,
    ): Boolean {
        return attemptCorrectingScraper(oldScraper, modifications)
    }

    private suspend fun attemptCorrectingScraper(
        oldScraper: IScraperData,
        modifications: List<Modification<Element>>
    ): Boolean {
        // Get the fixed script from the LLM
        val newScript = when (model.modelName) {
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

        // First close the current classloader to avoid any leaks
        closeCurrent()

        // Make sure we wait for any lingering references to be cleaned up
        System.gc()
        Thread.sleep(100) // Small pause to allow GC to work

        val newScraperResult =
            ScraperCompiler.attemptToCompileAndInstantiate(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", driver, snapshotService)

        if (newScraperResult == null) {
            println("Compilation of the new scraper failed")
            val oldScript = persistenceService.read(Configurations.versioningBaseDir + "${oldScraper.name}.kt")
            persistenceService.write(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", oldScript)

            return false
        }

        // currentScraper is now the newly compiled one
        currentScraper = newScraperResult

        backupScraper = currentScraper

        val currentScraperValue = currentScraper ?: throw IllegalStateException("Current scraper is null.")

        val success = testScraper(currentScraperValue)
        if (!success) {
            // Revert currentScraper to the backup scraper
            closeCurrent()
            currentScraper = backupScraper
            println("Scraper tests failed.")
            return false
        }

        // Close the backup CL and attempt to eliminate it from memory
        closeBackup()

        // Save the current scraper as the new backup
        backupScraper = currentScraper

        println("Scraper tests were successful!")
        return true
    }

    /**
     * Runs the given scraper.
     *
     * @param snapshotsPath The path to the snapshots' directory.
     */
    private suspend fun runScraper(snapshotsPath: String): Boolean {
        try {
            currentScraper?.scraper?.scrape()
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
                    val currentScraperValue = currentScraper ?: throw IllegalStateException("Current scraper is null")
                    val modifications = getModifications(currentScraperValue.scraper.getScraperData(), stepName)
                    val wasSuccessful = correctScraper(currentScraperValue.scraper.getScraperData(), modifications)

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
     * Tests the given CompiledScraperResult.
     * This updated method takes the CompiledScraperResult directly rather than just the IScraper instance
     * to ensure we're using the correct classloader for both the test class and scraper.
     *
     * @param compiledResult The compiled scraper result containing both the scraper and its classloader
     */
    private fun testScraper(compiledResult: CompiledScraperResult): Boolean {
        println("Testing scraper with proper classloader...")

        // Use the same classloader for both test class and scraper interface
        val classLoader = compiledResult.classLoader
        val scraper = compiledResult.scraper

        // Load test class from the same classloader as the scraper
        val testClass = requireNotNull(classLoader.loadClass(scraperTestClassName)) {
            "Failed to load test class $scraperTestClassName"
        }

        // Load IScraper interface from the same classloader
        val scraperInterface = requireNotNull(classLoader.loadClass("interfaces.IScraper")) {
            "Failed to load IScraper interface from same classloader"
        }

        // Verify that the scraper is indeed an instance of the correct IScraper interface
        if (!scraperInterface.isInstance(scraper)) {
            println("WARNING: Scraper is not an instance of the expected IScraper interface!")
            println("Scraper class: ${scraper.javaClass.name}")
            println("Expected interface: ${scraperInterface.name}")
            println("Scraper's classloader: ${scraper.javaClass.classLoader}")
            println("Interface's classloader: ${scraperInterface.classLoader}")
            return false
        }

        // Create the test instance with the correct scraper
        val testInstance = testClass.getDeclaredConstructor(scraperInterface).newInstance(scraper)

        // Run setup methods
        getSetUpMethods(testClass).forEach { it.invoke(testInstance) }

        val testMethods = testClass.methods.filter {
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
            }
        }

        getTearDownMethods(testClass)
            .forEach { it.invoke(testInstance) }

        return failedTests == 0
    }

    private fun getSetUpMethods(clazz: Class<*>): List<Method> =
        clazz.methods.filter { it.isAnnotationPresent(org.junit.jupiter.api.BeforeAll::class.java) }

    private fun getTearDownMethods(clazz: Class<*>): List<Method> =
        clazz.methods.filter { it.isAnnotationPresent(org.junit.jupiter.api.AfterAll::class.java) }


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

    override fun close() {
        closeCurrent()
        closeBackup()
        System.gc()
    }

    override suspend fun scrape(): Boolean {
        var attempts = 0
        var success = false

        while (!success && attempts < retries) {
            val currentScraperValue = currentScraper ?: throw IllegalStateException("Current scraper is null")
            success = runScraper(Configurations.snapshotBaseDir + currentScraperValue.scraper::class.simpleName + "/latest")
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

    private fun logMemoryAndClassloaderInfo(label: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)

        println("======= $label =======")
        println("Memory usage: $usedMemory MB / $totalMemory MB")
        println("Current classloader: ${this.javaClass.classLoader}")
        println("Current scraper classloader: ${currentScraper?.scraper?.javaClass?.classLoader}")
        println("Backup scraper classloader: ${backupScraper?.scraper?.javaClass?.classLoader}")

        // List loaded classes (for debugging severe issues)
        val beans = ManagementFactory.getPlatformMBeanServer()
        val mbean = ManagementFactory.newPlatformMXBeanProxy(
            beans, "com.sun.management:type=ClassLoading",
            ClassLoadingMXBean::class.java
        )

        println("Total loaded classes: ${mbean.loadedClassCount}")
        println("===========================")
    }
}
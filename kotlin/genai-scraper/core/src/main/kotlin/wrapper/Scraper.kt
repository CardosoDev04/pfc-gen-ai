package wrapper

import Configurations
import classes.data.Element
import classes.llm.LLM
import classes.service_model.Modification
import compiler.ScraperCompiler
import domain.model.interfaces.IScraperWrapper
import domain.prompts.FEW_SHOT_SCRAPER_UPDATE_MESSAGES
import enums.ScraperCorrectionResult
import html_fetcher.WebExtractor
import interfaces.IScraper
import interfaces.IScraperData
import modification_detection.IModificationDetectionService
import org.openqa.selenium.ElementNotInteractableException
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import persistence.PersistenceService
import snapshots.ISnapshotService
import steptracker.StepTracker
import java.io.File
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
    private var backupScraper: IScraper?,
    private var currentScraper: IScraper,
    private val retries: Int,
    private val model: LLM
) : IScraperWrapper {

    override suspend fun scrape(): Boolean {
        val wasSuccessful = runScraper(Configurations.snapshotBaseDir + currentScraper::class.simpleName + "/latest")

        if (!wasSuccessful) {
            println("Max retries reached.")
        }

        return wasSuccessful
    }

    /**
     * Runs the given scraper.
     *
     * @param snapshotsPath The path to the snapshots' directory.
     */
    private suspend fun runScraper(snapshotsPath: String): Boolean {
        try {
            currentScraper.scrape()
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

                    val wasSuccessful = correctScraper(currentScraper, stepName)

                    if (!wasSuccessful) {
                        println("Scraper correction failed.")
                    }

                    return wasSuccessful
                }
            }

            throw e
        }
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

    private suspend fun correctScraper(
        oldScraper: IScraper,
        stepName: String
    ): Boolean {
        var attempts = 0

        while (attempts < retries) {
            val currentScraperData = currentScraper.getScraperData()

            val modifications = getModifications(currentScraperData, stepName)
            val result = attemptCorrectingScraper(currentScraperData, modifications)

            if (result == ScraperCorrectionResult.Success) {
                println("Scraper correction successful")
                break
            } else if (result == ScraperCorrectionResult.PartialFix) {
                println("Scraper partially fixed")
            } else if (result == ScraperCorrectionResult.Failure) {
                println("Retry #${attempts + 1} failed.")
                attempts++
                currentScraper = oldScraper
            }
        }

        return attempts < retries
    }

    private suspend fun attemptCorrectingScraper(
        oldScraper: IScraperData,
        modifications: List<Modification<Element>>
    ): ScraperCorrectionResult {
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

        val newScraperResult = ScraperCompiler.attemptToCompileAndInstantiate(oldScraper.name, newScript, driver, snapshotService, scraperTestClassName)
            ?: return ScraperCorrectionResult.Failure

        backupScraper = currentScraper
        currentScraper = newScraperResult.scraperInstance

        val success = testScraper(newScraperResult.testInstance)

        if (!success) {

            if (wasPartiallyFixed()) {
                // Write new code to the scraper file here because getScraperData() reads the code from the scrapers directory
                persistenceService.write(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", newScript)
                return ScraperCorrectionResult.PartialFix
            }

            // Revert currentScraper to the backup scraper
            val backupScraperValue = backupScraper ?: throw IllegalStateException("Backup scraper is null.")
            currentScraper = backupScraperValue
            return ScraperCorrectionResult.Failure
        }

        // Save the current scraper as the new backup
        backupScraper = currentScraper

        // Overwrite scraper's source code
        persistenceService.write(Configurations.scrapersBaseDir + "${oldScraper.name}.kt", newScript)

        return ScraperCorrectionResult.Success
    }

    /**
     * Tests the given CompiledScraperResult.
     * This updated method takes the CompiledScraperResult directly rather than just the IScraper instance
     * to ensure we're using the correct classloader for both the test class and scraper.
     *
     * @param testInstance The compiled scraper test instance
     */
    private fun testScraper(testInstance: Any): Boolean {
        println("Testing scraper...")

        getSetUpMethods(testInstance::class)
            .forEach { it.invoke(testInstance) }

        // Run all tests from the test class
        val testMethods = testInstance::class.java.methods.filter {
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

        getTearDownMethods(testInstance::class)
            .forEach { it.invoke(testInstance) }

        return failedTests == 0
    }

    /**
     * Checks if the scraper was partially fixed by comparing the number of steps completed of the last two runs.
     * @return true if the last run had more steps completed than the second last run, meaning it was partially fixed, false otherwise.
     */
    private fun wasPartiallyFixed(): Boolean {
        val (secondLastRunSteps, lastRunSteps) = StepTracker.getTwoLastRuns()
        return lastRunSteps > secondLastRunSteps
    }

    private fun getTearDownMethods(clazz: KClass<*>): List<Method> =
        clazz.java.methods.filter {
            it.isAnnotationPresent(org.junit.jupiter.api.AfterAll::class.java)
        }

    private fun getSetUpMethods(clazz: KClass<*>): List<Method> =
        clazz.java.methods.filter {
            it.isAnnotationPresent(org.junit.jupiter.api.BeforeAll::class.java)
        }
}
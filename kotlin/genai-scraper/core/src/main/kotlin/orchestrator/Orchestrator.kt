package orchestrator

import classes.data.Element
import classes.service_model.Snapshot
import domain.interfaces.ITestReportService
import domain.model.interfaces.IOrchestrator
import domain.model.interfaces.IScraper
import html_fetcher.WebExtractor
import modification_detection.IModificationDetectionService
import snapshots.ISnapshotService
import java.io.File
import java.net.URLClassLoader
import java.util.*
import javax.tools.ToolProvider

class Orchestrator(
    private val modificationDetectionService: IModificationDetectionService,
    private val snapshotService: ISnapshotService,
    private val testReportService: ITestReportService,
    private val webExtractor: WebExtractor
) : IOrchestrator {

    /**
     * Corrects the scraper by identifying modifications and recompiling the scraper.
     *
     * @param oldScraper The old scraper instance.
     * @param retries The number of retries allowed if the scraper fails tests.
     */
    override suspend fun correctScraper(oldScraper: IScraper, retries: Int) {
        val latestSnapshot = snapshotService.getSnapshot("TODO()")
        val latestStableSnapshot = snapshotService.getSnapshot("TODO()")
        val latestSnapshotHtml = latestSnapshot.html.readText()
        val latestStableSnapshotHtml = latestStableSnapshot.html.readText()

        val modifiedElements =
            modificationDetectionService.getMissingElements(latestStableSnapshotHtml, latestSnapshotHtml)
        val newElements = emptyList<Element>() // TODO()
        val modifications = modifiedElements.map { modificationDetectionService.getModification(it, newElements) }

        val newScript = modificationDetectionService.modifyScript(oldScraper.code, modifications)

        val newScraper = compileAndInstantiateNewScraper("TODO()")

        try {
            testScraper(newScraper)
        } catch (e: Exception) {
            println("Scraper did not pass automated tests. Retries left: $retries")
            if (retries - 1 > 0) correctScraper(oldScraper, retries - 1)
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
        val kotlinCompiler = ToolProvider.getSystemJavaCompiler()
        val file = File(scraperCodePath)
        val compilationResult = kotlinCompiler.run(null, null, null, file.path)
        if (compilationResult != 0) {
            throw RuntimeException("Failed to compile Kotlin file: $scraperCodePath")
        }

        val classLoader = URLClassLoader.newInstance(arrayOf(file.parentFile.toURI().toURL()))
        val className =
            file.nameWithoutExtension.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val clazz = classLoader.loadClass(className)

        val scraperInstance = clazz.getDeclaredConstructor().newInstance() as IScraper
        return scraperInstance
    }

    /**
     * Runs the given scraper.
     *
     * @param scraper The scraper instance to run.
     */
    override suspend fun runScraper(scraper: IScraper) {
        TODO("Not yet implemented")
    }

    /**
     * Tests the given scraper.
     *
     * @param scraper The scraper instance to test.
     */
    override suspend fun testScraper(scraper: IScraper) {
        TODO("Not yet implemented")
    }
}
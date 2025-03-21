package orchestrator

import classes.data.Element
import domain.interfaces.ITestReportService
import domain.model.interfaces.IOrchestrator
import domain.model.interfaces.IScraper
import html_fetcher.WebExtractor
import modification_detection.IModificationDetectionService
import snapshots.ISnapshotService

class Orchestrator(
    private val modificationDetectionService: IModificationDetectionService,
    private val snapshotService: ISnapshotService,
    private val testReportService: ITestReportService,
    private val webExtractor: WebExtractor
): IOrchestrator {
    override suspend fun correctScraper(oldScraper: IScraper, retries: Int) {
        val latestSnapshot = snapshotService.getSnapshot("TODO()")
        val latestStableSnapshot = snapshotService.getSnapshot("TODO()")
        val latestSnapshotHtml = latestSnapshot.html.readText()
        val latestStableSnapshotHtml = latestStableSnapshot.html.readText()

        val modifiedElements = modificationDetectionService.getMissingElements(latestStableSnapshotHtml, latestSnapshotHtml)
        val newElements = TODO()  // NEED TO CHANGE ONCE WEB EXTRACTOR IS FINISHED
        val modifications = modifiedElements.map { modificationDetectionService.getModification(it, newElements) }

        // NEED TO CHANGE ONCE WE HAVE OVERLOAD
        val newScript = TODO()

        val newScraper = compileAndInstantiateNewScraper(TODO())

        try {
            testScraper(newScraper)
        } catch (e: Exception) {
            println("Scraper did not pass automated tests. Retries left: $retries")
            if(retries - 1 > 0) correctScraper(oldScraper, retries -1)
        }
    }

    override suspend fun compileAndInstantiateNewScraper(scraperCodePath: String): IScraper {
        TODO("Not yet implemented")
    }

    override suspend fun runScraper(scraper: IScraper) {
        TODO("Not yet implemented")
    }

    override suspend fun testScraper(scraper: IScraper) {
        TODO("Not yet implemented")
    }
}
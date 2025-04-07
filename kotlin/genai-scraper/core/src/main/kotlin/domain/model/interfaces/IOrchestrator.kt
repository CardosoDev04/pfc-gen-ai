package domain.model.interfaces

import interfaces.IScraper
import interfaces.IScraperData

interface IOrchestrator {
    suspend fun correctScraper(oldScraper: IScraperData, stepName: String, retries: Int = 3)
    suspend fun compileAndInstantiateNewScraper(scraperCodePath: String): IScraper
    suspend fun runScraper(scraper: IScraperData, snapshotsPath: String)
    suspend fun testScraper(scraper: IScraper)
}
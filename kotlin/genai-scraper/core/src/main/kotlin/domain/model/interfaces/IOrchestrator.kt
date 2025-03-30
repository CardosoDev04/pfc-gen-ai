package domain.model.interfaces

import classes.scrapers.DemoScraperDataBundle
import interfaces.IScraper
import interfaces.IScraperData

interface IOrchestrator {
    suspend fun correctScraper(oldScraper: IScraperData, stepName: String, retries: Int = 3)
    suspend fun compileAndInstantiateNewScraper(scraperCodePath: String): IScraper
    suspend fun runDemoScraper(scraper: DemoScraperDataBundle)
    suspend fun testScraper(scraper: IScraper)
}
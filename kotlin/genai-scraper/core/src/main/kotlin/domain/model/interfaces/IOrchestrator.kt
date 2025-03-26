package domain.model.interfaces

import classes.scrapers.DemoScraperBundle
import interfaces.IScraper

interface IOrchestrator {
    suspend fun correctScraper(oldScraper: IScraper, stepName: String, retries: Int = 3)
    suspend fun compileAndInstantiateNewScraper(scraperCodePath: String): IScraper
    suspend fun runDemoScraper(scraper: DemoScraperBundle)
    suspend fun testScraper(scraper: IScraper)
}
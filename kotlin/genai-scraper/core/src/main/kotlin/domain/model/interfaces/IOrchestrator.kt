package domain.model.interfaces

import classes.data.Element
import classes.service_model.Modification
import interfaces.IScraper
import interfaces.IScraperData

interface IOrchestrator {
    suspend fun correctScraper(oldScraper: IScraperData, modifications: List<Modification<Element>>, retries: Int = 3): Boolean
    suspend fun attemptToCompileAndInstantiateNewScraper(scraperCodePath: String): IScraper?
    suspend fun runScraper(scraper: IScraperData, snapshotsPath: String)
    suspend fun testScraper(scraper: IScraper): Boolean
}
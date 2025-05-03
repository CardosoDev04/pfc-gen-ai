package domain.model.interfaces

import classes.data.Element
import classes.service_model.Modification
import domain.model.classes.data.CompiledScraperResult
import interfaces.IScraper
import interfaces.IScraperData
import library.wrappers.GenericScraper

interface IOrchestrator {
    suspend fun correctScraper(oldScraper: IScraperData, modifications: List<Modification<Element>>,wrapper: GenericScraper, retries: Int = 3): Boolean
    suspend fun attemptToCompileAndInstantiateNewScraper(scraperCodePath: String): CompiledScraperResult?
    suspend fun runScraper(wrapper: GenericScraper, snapshotsPath: String) : Boolean
    suspend fun testScraper(scraper: IScraper): Boolean
}
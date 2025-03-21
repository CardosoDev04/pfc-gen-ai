package domain.model.interfaces

interface IOrchestrator {
    suspend fun correctScraper(oldScraper: IScraper, retries: Int = 3)
    suspend fun compileAndInstantiateNewScraper(scraperCodePath: String): IScraper
    suspend fun runScraper(scraper: IScraper)
    suspend fun testScraper(scraper: IScraper)
}
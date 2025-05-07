package interfaces

interface IScraper {
    suspend fun scrape(): Any
    fun getScraperData(): IScraperData
}
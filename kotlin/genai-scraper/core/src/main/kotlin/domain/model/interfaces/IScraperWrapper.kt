package domain.model.interfaces

interface IScraperWrapper {
    suspend fun scrape(): Boolean
}
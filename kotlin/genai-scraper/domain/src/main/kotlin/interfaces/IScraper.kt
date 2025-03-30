package interfaces

interface IScraper {
    suspend fun scrape(): Any
}
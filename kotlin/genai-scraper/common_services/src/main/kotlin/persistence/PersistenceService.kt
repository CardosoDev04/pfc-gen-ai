package persistence


interface PersistenceService {
    fun write(modelName: String, fileName: String, fileContent: String)
    fun read(scraperPath: String): String
}
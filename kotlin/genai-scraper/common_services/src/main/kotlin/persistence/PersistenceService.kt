package persistence


interface PersistenceService {
    fun write(filePath: String, content: String)
    fun write(modelName: String, fileName: String, fileContent: String)
    fun read(scraperPath: String): String
}
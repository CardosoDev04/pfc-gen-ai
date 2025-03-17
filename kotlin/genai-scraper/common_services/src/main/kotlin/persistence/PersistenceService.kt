package persistence

import classes.llm.Model

interface PersistenceService {
    fun write(modelName: String, fileContent: String)
    fun read(scraperPath: String): String
}
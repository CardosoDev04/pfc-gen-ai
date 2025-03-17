package persistence

import classes.llm.Model

interface PersistenceService {
    fun write(model: Model, fileContent: String)
    fun read(scraperPath: String): String
}
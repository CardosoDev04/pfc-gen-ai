package persistence

import classes.llm.Model

interface PersistenceService {
    fun write(model: Model, fileContent: String)
    fun read(scraperName: String): String
}
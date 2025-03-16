package persistence.implementations

import classes.llm.Model
import persistence.PersistenceService

class FilePersistenceService : PersistenceService {
    override fun write(model: Model, fileContent: String) {
        TODO("Not yet implemented")
    }

    override fun read(scraperName: String): String {
        TODO("Not yet implemented")
    }
}
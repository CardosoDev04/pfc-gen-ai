package persistence.implementations

import classes.llm.Model
import persistence.PersistenceService
import java.io.File

class FilePersistenceService : PersistenceService {
    override fun write(model: Model, fileContent: String) {
        TODO("Not yet implemented")
    }

    override fun read(scraperPath: String): String {
        return File(scraperPath).readText()
    }
}
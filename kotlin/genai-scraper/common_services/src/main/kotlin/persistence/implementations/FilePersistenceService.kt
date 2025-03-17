package persistence.implementations

import classes.llm.LLM
import classes.llm.Model
import persistence.PersistenceService
import java.io.File

class FilePersistenceService(
    private val resultsBaseDir: String = System.getProperty("user.dir") + "/results"
) : PersistenceService {
    override fun write(modelName: String, fileContent: String) {
        val latestDirectory = File("${resultsBaseDir}/${modelName}/latest")
        if (latestDirectory.exists() && latestDirectory.isDirectory) {
            // TODO("Cut the contents from this directory and paste them into the appropriate timestamp")
        } else {
            // TODO("If the directory does not exist create it")
        }

        // TODO("Write the file content to the latest directory")
    }

    override fun read(scraperPath: String): String {
        return File(scraperPath).readText()
    }
}

fun main() {
    val fps = FilePersistenceService()
    fps.write(LLM.Mistral7B.modelName, "Hello World!")
}
package persistence.implementations

import persistence.PersistenceService
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A service for persisting files to the filesystem.
 *
 * @property resultsBaseDir The base directory where results are stored.
 */
class FilePersistenceService(
    private val resultsBaseDir: String = System.getProperty("user.dir") + "/results"
) : PersistenceService {

    /**
     * Writes the given content to a file.
     *
     * @param modelName The name of the model.
     * @param fileName The name of the file.
     * @param fileContent The content to write to the file.
     */
    override fun write(modelName: String, fileName: String, fileContent: String) {
        val latestDirectory = File("${resultsBaseDir}/${modelName}/latest")
        if (latestDirectory.exists() && latestDirectory.isDirectory) {
            copyDirectory(latestDirectory, File("${resultsBaseDir}/${modelName}/${getCurrentTimestamp()}"))
        } else {
            latestDirectory.mkdirs()
        }

        val file = File(latestDirectory, fileName)
        file.createNewFile()
        file.writeText(fileContent)
    }

    /**
     * Reads the content of a file.
     *
     * @param scraperPath The path to the file.
     * @return The content of the file.
     */
    override fun read(scraperPath: String): String {
        return File(scraperPath).readText()
    }

    /**
     * Copies the contents of one directory to another.
     *
     * @param sourceDir The source directory.
     * @param destDir The destination directory.
     */
    private fun copyDirectory(sourceDir: File, destDir: File) {
        val sourceDirFiles = sourceDir.listFiles()

        if (!destDir.exists() && sourceDirFiles?.size != 0) {
            destDir.mkdirs()
        }

        sourceDirFiles?.forEach { file ->
            val destFile = File(destDir, file.name)
            if (file.isDirectory()) {
                copyDirectory(file, destFile)
                file.delete()
            } else {
                file.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                file.delete()
            }
        }
    }

    /**
     * Gets the current timestamp in the format `yyyyMMdd_HHmmSS`.
     *
     * @return The current timestamp.
     */
    private fun getCurrentTimestamp(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmSS").withZone(ZoneId.systemDefault())
        return formatter.format(Instant.now())
    }
}
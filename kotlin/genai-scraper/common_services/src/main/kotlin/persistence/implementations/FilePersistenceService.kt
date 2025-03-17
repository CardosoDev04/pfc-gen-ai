package persistence.implementations

import persistence.PersistenceService
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FilePersistenceService(
    private val resultsBaseDir: String = System.getProperty("user.dir") + "/results"
) : PersistenceService {
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

    override fun read(scraperPath: String): String {
        return File(scraperPath).readText()
    }

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

    private fun getCurrentTimestamp(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmSS").withZone(ZoneId.systemDefault())
        return formatter.format(Instant.now())
    }
}
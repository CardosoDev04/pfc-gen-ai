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
    override fun write(filePath: String, content: String) {
        val file = File(filePath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        file.writeText(content)
    }

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

    override fun copyAndDeleteFile(from: String, to: String) {
        val fileContent = read(from)
        write(to, fileContent)
        deleteFile(from)
    }

    override fun copyWholeDirectory(from: String, to: String) {
        val sourceDir = File(from)
        val destDir = File(to)
        if (!sourceDir.exists() || !sourceDir.isDirectory) return

        copyRecursively(sourceDir, destDir)
    }

    override fun deleteAllContents(parentPath: String) {
        val parentDir = File(parentPath)
        if (!parentDir.exists() || !parentDir.isDirectory) return

        parentDir.listFiles()?.forEach { file ->
            file.deleteRecursively()
        }
    }

    override fun findLastCreatedDirectory(directoryPath: String): File? {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            throw IllegalArgumentException("The provided path is not a valid directory")
        }

        return directory.listFiles { file -> file.isDirectory }?.maxByOrNull { it.lastModified() }
    }

    private fun copyRecursively(src: File, dst: File) {
        if (src.isDirectory) {
            if (!dst.exists()) dst.mkdirs()
            src.listFiles()?.forEach { child ->
                copyRecursively(child, File(dst, child.name))
            }
        } else {
            src.inputStream().use { input ->
                dst.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun deleteFile(path: String) {
        val toDelete = File(path)

        if (toDelete.exists()) {
            toDelete.delete()
        }
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
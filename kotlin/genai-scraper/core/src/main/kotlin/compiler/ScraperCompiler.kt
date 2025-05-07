package compiler

import Configurations
import domain.model.classes.data.CompiledScraperResult
import interfaces.IScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openqa.selenium.WebDriver
import snapshots.ISnapshotService
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object ScraperCompiler {
    suspend fun attemptToCompileAndInstantiate(scraperCodePath: String, driver: WebDriver, snapshotService: ISnapshotService): CompiledScraperResult? =
        withContext(Dispatchers.IO) {
            try {
                val originalFile = File(scraperCodePath)

                // Clear the versioning folder
                val versioningBaseDir = Paths.get(Configurations.versioningBaseDir)
                if (Files.exists(versioningBaseDir)) {
                    Files.walk(versioningBaseDir)
                        .sorted(Comparator.reverseOrder())
                        .map { it.toFile() }
                        .forEach { it.delete() }
                }

                // Create a new versioning folder
                val tempFolder = File("${Configurations.versioningBaseDir}/compiled_${System.currentTimeMillis()}")
                tempFolder.mkdirs()

                // Copy the Kotlin file to the versioning folder
                val tempKtFile = File(tempFolder, originalFile.name)
                originalFile.copyTo(tempKtFile)

                // Compile to the versioning folder
                val classpath = System.getProperty("java.class.path")
                val process = ProcessBuilder("kotlinc", tempKtFile.path, "-d", tempFolder.path, "-classpath", classpath)
                    .redirectErrorStream(true)
                    .start()

                val compilerOutput = process.inputStream.bufferedReader().readText()
                println("Compiler output:\n$compilerOutput")

                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    println("Compilation failed for ${tempKtFile.name}")
                    return@withContext null
                }

                val newClassLoader =
                    URLClassLoader.newInstance(arrayOf(tempFolder.toURI().toURL()), this::class.java.classLoader)
                val className = tempKtFile.nameWithoutExtension.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                val clazz = newClassLoader.loadClass("scrapers.$className")

                return@withContext CompiledScraperResult(
                    clazz.getDeclaredConstructor(WebDriver::class.java, ISnapshotService::class.java)
                        .newInstance(driver, snapshotService) as IScraper,
                    newClassLoader
                )

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
}

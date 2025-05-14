package compiler

import Configurations
import interfaces.IScraper
import org.openqa.selenium.WebDriver
import snapshots.ISnapshotService
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.time.LocalDateTime

data class CompiledScraperResult(
    val scraperInstance: IScraper,
    val testInstance: Any
)

class IsolatedClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("scrapers.")) {
            val loaded = findLoadedClass(name) ?: findClass(name)
            if (resolve) resolveClass(loaded)
            return loaded
        }
        return super.loadClass(name, resolve)
    }
}


object ScraperCompiler {
    fun attemptToCompileAndInstantiate(
        scraperName: String,
        newScraperCode: String,
        driver: WebDriver,
        snapshotService: ISnapshotService,
        testClassName: String
    ): CompiledScraperResult? {
        return try {
            val srcDir = File(Configurations.versioningBaseDir + "src/${scraperName}_${LocalDateTime.now()}")
            srcDir.mkdirs()

            // Create scraper file
            val newScraperFile = File(srcDir, "$scraperName.kt")
            newScraperFile.writeText(newScraperCode)

            // Copy test file from static location into srcDir
            val testFileName = "$testClassName.kt"
            val existingTestFile = File(Configurations.scrapersBaseDir, testFileName)

            if (!existingTestFile.exists()) {
                println("Test file not found at ${existingTestFile.absolutePath}")
                return null
            }

            val destTestFile = File(srcDir, testFileName)
            existingTestFile.copyTo(destTestFile, overwrite = true)

            val compiledDir = File(Configurations.versioningBaseDir + "out/${scraperName}_${LocalDateTime.now()}")
            compiledDir.mkdirs()
            val isScraperCompileSuccess = compileKotlin(srcDir, compiledDir)

            if (!isScraperCompileSuccess) {
                println("Dynamic scraper compilation failed")
                return null
            }

            val classLoader = IsolatedClassLoader(arrayOf(compiledDir.toURI().toURL()), javaClass.classLoader)

            val scraperClass = classLoader.loadClass("scrapers.${scraperName}")
            val scraperInstance = scraperClass
                .getDeclaredConstructor(WebDriver::class.java, ISnapshotService::class.java)
                .newInstance(driver, snapshotService) as IScraper

            val testClass = classLoader.loadClass("scrapers.$testClassName")
            val testInstance = testClass
               // .also { classLoader.close() }
                .getDeclaredConstructor(IScraper::class.java)
                .newInstance(scraperInstance)

            CompiledScraperResult(scraperInstance, testInstance)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compileKotlin(
        sourceDir: File,
        outputDir: File,
        kotlincPath: String = "kotlinc"
    ): Boolean {
        val sourceFiles = sourceDir.walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.absolutePath }
            .toList()

        if (sourceFiles.isEmpty()) {
            println("No Kotlin source files to compile.")
            return false
        }

        val process = ProcessBuilder().apply {
            command(
                kotlincPath,
                "-d", outputDir.absolutePath,
                "-no-stdlib",
                "-no-reflect",
                "-cp", System.getProperty("java.class.path"),
                *sourceFiles.toTypedArray()
            )
            inheritIO()
        }.start()

        val exitCode = process.waitFor()
        println("kotlinc exited with $exitCode")
        return exitCode == 0
    }

}
package compiler

import interfaces.IScraper
import org.openqa.selenium.WebDriver
import snapshots.ISnapshotService
import java.io.File
import java.net.URL
import java.net.URLClassLoader

data class CompiledScraperResult(
    val scraperInstance: IScraper,
    val testInstance: Any
)

class IsolatedClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("scraper.")) {
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
        scraperSourceDir: File,
        testSourceDir: File,
        outputDir: File,
        driver: WebDriver,
        snapshotService: ISnapshotService
    ): CompiledScraperResult? {
        return try {
            if (!scraperSourceDir.exists() || !testSourceDir.exists()) {
                println("Source directories not found")
                return null
            }

            outputDir.mkdirs()

            val allSourceFiles = (scraperSourceDir.walkTopDown())
                .filter { it.extension == "kt" }
                .toList()

            if (allSourceFiles.isEmpty()) {
                println("No Kotlin source files to compile.")
                return null
            }

            val compileSuccess = compileKotlin(allSourceFiles, outputDir)
            if (!compileSuccess) {
                println("Dynamic scraper/test compilation failed")
                return null
            }

            val classLoader = IsolatedClassLoader(arrayOf(outputDir.toURI().toURL()), javaClass.classLoader)

            val scraperClass = classLoader.loadClass("scraper.$scraperName")
            val scraperInstance = scraperClass
                .getDeclaredConstructor(WebDriver::class.java, ISnapshotService::class.java)
                .newInstance(driver, snapshotService) as IScraper

            val testClassName = testSourceDir.walkTopDown()
                .firstOrNull { it.extension == "kt" }
                ?.nameWithoutExtension
                ?: return null

            val testClass = classLoader.loadClass("scraper.$testClassName")
            val testInstance = testClass
                .getDeclaredConstructor(IScraper::class.java)
                .newInstance(scraperInstance)

            CompiledScraperResult(scraperInstance, testInstance)
        } catch (e: Exception) {
            println("Compilation or instantiation failed:")
            e.printStackTrace()
            null
        }
    }

    private fun compileKotlin(
        sourceFiles: List<File>,
        outputDir: File,
        kotlincPath: String = "kotlinc"
    ): Boolean {
        val process = ProcessBuilder().apply {
            command(
                kotlincPath,
                "-d", outputDir.absolutePath,
                "-no-stdlib",
                "-no-reflect",
                "-cp", System.getProperty("java.class.path"),
                *sourceFiles.map { it.absolutePath }.toTypedArray()
            )
            inheritIO()
        }.start()

        val exitCode = process.waitFor()
        println("kotlinc exited with $exitCode")
        return exitCode == 0
    }
}

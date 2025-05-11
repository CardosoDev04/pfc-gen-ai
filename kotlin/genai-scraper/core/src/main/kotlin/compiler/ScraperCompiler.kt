package compiler

import Configurations
import interfaces.IScraper
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.openqa.selenium.WebDriver
import snapshots.ISnapshotService
import java.io.File
import java.net.URLClassLoader
import java.time.LocalDateTime

data class CompiledScraperResult(
    val scraperInstance: IScraper,
    val testInstance: Any
)

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

            val newSrcFile = File(srcDir, scraperName)
            newSrcFile.writeText(newScraperCode)

            val compiledDir = File(Configurations.versioningBaseDir + "out/${scraperName}_${LocalDateTime.now()}")
            compiledDir.mkdirs()
            compileKotlin(srcDir, compiledDir)

            val classLoader = URLClassLoader(arrayOf(compiledDir.toURI().toURL()))

            val scraperClass = classLoader.loadClass("scrapers.${scraperName}")
            val scraperInstance = scraperClass
                .getDeclaredConstructor(WebDriver::class.java, ISnapshotService::class.java)
                .newInstance(driver, snapshotService) as IScraper

            val testClass = classLoader.loadClass("scrapers.$testClassName")
            val testInstance = testClass
                .also { classLoader.close() }
                .getDeclaredConstructor(IScraper::class.java)
                .newInstance(scraperInstance)

            CompiledScraperResult(scraperInstance, testInstance)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compileKotlin(sourceDir: File, outputDir: File): Boolean {
        val args = mutableListOf<String>().apply {
            add("-d")
            add(outputDir.absolutePath)
            add("-classpath")
            add(System.getProperty("java.class.path"))
            add("-no-stdlib")
            add("-no-reflect")
            addAll(
                sourceDir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .map { it.absolutePath }
            )
        }

        return CLICompiler.doMainNoExit(K2JVMCompiler(), args.toTypedArray()) == ExitCode.OK
    }
}
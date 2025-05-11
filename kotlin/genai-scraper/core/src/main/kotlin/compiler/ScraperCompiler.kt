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

object ScraperCompiler {
    fun attemptToCompileAndInstantiate(scraperCodePath: String, driver: WebDriver, snapshotService: ISnapshotService): IScraper? {
        return try {
            val scraperCode = File(scraperCodePath).readText()

            val srcDir = File(Configurations.versioningBaseDir + "/src/" + LocalDateTime.now().toString())
            srcDir.mkdirs()

            val newSrcFile = File(srcDir, scraperCodePath.substringAfterLast("/"))
            newSrcFile.writeText(scraperCode)

            val compiledDir = File(Configurations.versioningBaseDir + "/out/" + LocalDateTime.now().toString())
            compiledDir.mkdirs()
            compileKotlin(srcDir, compiledDir)

            val classLoader =
                URLClassLoader(arrayOf(compiledDir.toURI().toURL()), Thread.currentThread().contextClassLoader)
            val instance =
                classLoader.loadClass("scrapers.${scraperCodePath.substringAfterLast("/").substringBeforeLast(".")}")
                    .also { classLoader.close() }
                    .getDeclaredConstructor(WebDriver::class.java, ISnapshotService::class.java)
                    .newInstance(driver, snapshotService) as IScraper

            instance
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
package snapshots

import classes.service_model.Snapshot
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * A service for taking and managing snapshots of web pages.
 */
class SnapshotService(private val className: String): ISnapshotService {
    override var isFirstRun = true
    private var currentStepN = 1

    // Clear the snapshots directory
    init {
        val snapshotsDir = File("${Configurations.snapshotBaseDir}/$className/latest")
        if (!snapshotsDir.exists() || !snapshotsDir.isDirectory) {
            throw IllegalStateException("Snapshots directory for $className not found")
        }

        snapshotsDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    override fun takeSnapshotAsFile(driver: WebDriver) {
        val basePath = if (isFirstRun) {
            Configurations.snapshotBaseDir + "/$className/latest/steps/$currentStepN"
        } else {
            Configurations.snapshotBaseDir + "/$className/test/steps/$currentStepN"
        }

        val htmlPath = "$basePath/html"

        val destFile = File("$basePath/screenshot.png")
        destFile.parentFile.mkdirs()
        val screenshot = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        Files.copy(screenshot.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val htmlFile = File("$htmlPath/index.html")
        htmlFile.parentFile.mkdirs()
        driver.pageSource?.let { htmlFile.writeText(it) }

        currentStepN++
    }

    override fun getSnapshot(htmlPath: String): Snapshot {
        val htmlFile = File(htmlPath)
        return Snapshot(htmlFile)
    }

    override fun resetCounter() {
        currentStepN = 1
    }
}
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
class SnapshotService: ISnapshotService {

    /**
     * Takes a snapshot of the current state of the web page and saves it as a file.
     *
     * @param driver The WebDriver instance used to take the snapshot.
     * @param path The path where the snapshot file will be saved.
     * @return The file containing the snapshot.
     */
    override fun takeSnapshotAsFile(driver: WebDriver, path: String): File {
        val destFile = File("$path/screenshot.png")
        destFile.parentFile.mkdirs()
        val screenshot = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        Files.copy(screenshot.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        // Saves the HTML
        val htmlFolderPath = "$path/html"
        val htmlFile = File("$htmlFolderPath/source.html")
        htmlFile.parentFile.mkdirs()
        driver.pageSource?.let { htmlFile.writeText(it) }

        return destFile
    }

    /**
     * Retrieves a snapshot from the given HTML file path.
     *
     * @param htmlPath The path to the HTML file.
     * @return The Snapshot object containing the HTML file.
     */
    override fun getSnapshot(htmlPath: String): Snapshot {
        val htmlFile = File(htmlPath)
        return Snapshot(htmlFile)
    }
}
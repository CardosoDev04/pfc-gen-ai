package snapshots

import com.cardoso.snapshots.ISnapshotService
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import java.io.File
import java.nio.file.Files

class SnapshotService: ISnapshotService {
    override fun takeSnapshotAsFile(driver: WebDriver, path: String): File {
        val destFile = File("$path/screenshot.png")
        destFile.parentFile.mkdirs()
        val screenshot = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        Files.copy(screenshot.toPath(), destFile.toPath())

        // Saves the HTML
        val htmlFolderPath = "$path/html"
        val htmlFile = File("$htmlFolderPath/source.html")
        htmlFile.parentFile.mkdirs()
        driver.pageSource?.let { htmlFile.writeText(it) }

        return destFile
    }
}
package snapshots

import org.openqa.selenium.WebDriver
import java.io.File
import domain.model.classes.data.Snapshot

interface ISnapshotService {
    fun takeSnapshotAsFile(driver: WebDriver, path: String): File
    fun getSnapshot(htmlPath: String): Snapshot
}
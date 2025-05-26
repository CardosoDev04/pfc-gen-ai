package snapshots

import classes.service_model.Snapshot
import org.openqa.selenium.WebDriver
import java.io.File

interface ISnapshotService {
    /**
     * Takes a snapshot of the current state of the web page and saves it as a file.
     *
     * @param driver The WebDriver instance used to take the snapshot.
     * @return The file containing the snapshot.
     */
    fun takeSnapshotAsFile(driver: WebDriver): File

    /**
     * Retrieves a snapshot from the given HTML file path.
     *
     * @param htmlPath The path to the HTML file.
     * @return The Snapshot object containing the HTML file.
     */
    fun getSnapshot(htmlPath: String): Snapshot
}
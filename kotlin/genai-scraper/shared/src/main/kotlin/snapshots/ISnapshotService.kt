package com.cardoso.snapshots

import classes.service_model.Snapshot
import org.openqa.selenium.WebDriver
import java.io.File

interface ISnapshotService {
    fun takeSnapshotAsFile(driver: WebDriver, path: String): File
    fun getSnapshot(htmlPath: String): Snapshot
}
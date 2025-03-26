package com.cardoso.snapshots

import org.openqa.selenium.WebDriver
import java.io.File

interface ISnapshotService {
    fun takeSnapshotAsFile(driver: WebDriver, path: String): File
}
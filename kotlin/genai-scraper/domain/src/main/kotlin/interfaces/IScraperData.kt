package interfaces

import java.io.File

interface IScraperData {
    val path: String
    val name: String get() = path.split("/").last().split(".").first()
    val code: String get() = File(path).readText()
    val compiledClass: IScraper
}

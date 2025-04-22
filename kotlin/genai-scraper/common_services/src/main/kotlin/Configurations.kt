import io.github.cdimascio.dotenv.dotenv

object Configurations {
    private val dotenv = dotenv()

    private val baseDir: String by lazy {
        dotenv["BASE_DIR"] ?: System.getProperty("user.dir")
    }

    val scrapersBaseDir: String by lazy {
        dotenv["SCRAPERS_DIR"] ?: "$baseDir/scrapers/src/main/kotlin/scrapers/"
    }

    val snapshotBaseDir: String by lazy {
        dotenv["SNAPSHOTS_DIR"] ?: "$baseDir/core/src/main/kotlin/snapshots/"
    }

    val tempBaseDir: String by lazy {
        dotenv["TEMP_DIR"] ?: "$baseDir/temp"
    }
}
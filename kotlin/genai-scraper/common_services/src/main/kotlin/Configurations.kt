import io.github.cdimascio.dotenv.dotenv

object Configurations {
    private val dotenv = dotenv()

    private val baseDir: String by lazy {
        dotenv["BASE_DIR"] ?: System.getProperty("user.dir")
    }

    val snapshotBaseDir: String by lazy {
        dotenv["SNAPSHOTS_DIR"] ?: "$baseDir/core/src/main/kotlin/snapshots/"
    }

    val versioningBaseDir: String by lazy {
        dotenv["TEMP_DIR"] ?: "$baseDir/versioning/"
    }
}
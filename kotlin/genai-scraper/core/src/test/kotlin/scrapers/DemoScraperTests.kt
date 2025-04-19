package scrapers

import com.cardoso.common.buildChromeDriver
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import snapshots.SnapshotService

class DemoScraperTests {
    @Test
    fun testScrape() {
        val demoScraper = DemoScraper(driver, snapshotService)
        runBlocking {
            demoScraper.scrape()
        }
    }

    companion object {
        private val driver = buildChromeDriver(true)
        private val snapshotService = SnapshotService()
    }
}
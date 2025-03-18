package test_report_service

import core.ExecutionTracker
import core.TestReportService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import utils.TimeStampService
import kotlin.test.assertEquals

class TestReportServiceTests {
    @Test
    fun `two recorded steps create a list with size two`() {
        runBlocking {
            executionTracker.recordStep("Sync work", syncWork)
            executionTracker.recordSuspendStep("Async work", asyncWork)

            assertEquals(2, executionTracker.getSteps().size)
        }
    }

    companion object {
        private lateinit var timeStampService: TimeStampService
        lateinit var executionTracker: ExecutionTracker
        private lateinit var testReportService: TestReportService
        lateinit var syncWork: () -> Unit
        lateinit var asyncWork: suspend () -> Unit

        @JvmStatic
        @BeforeAll
        fun setup() {
            timeStampService = TimeStampService()
            executionTracker = ExecutionTracker(timeStampService)
            testReportService = TestReportService(executionTracker, timeStampService)
            syncWork = { Thread.sleep(1000) }
            asyncWork = suspend { Thread.sleep(1000) }
        }
    }
}
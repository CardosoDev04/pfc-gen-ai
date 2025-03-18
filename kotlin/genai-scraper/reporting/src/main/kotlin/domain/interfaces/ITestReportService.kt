package domain.interfaces

import classes.service_model.TestReport

interface ITestReportService {
    fun generateTestReport(reportTitle: String): TestReport
}
package core

import classes.service_model.TestReport
import domain.interfaces.IExecutionTracker
import domain.interfaces.ITestReportService
import domain.interfaces.ITimeStampService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import utils.TimeStampService

class TestReportService (
    private val executionTracker: IExecutionTracker,
    private val timeStampService: ITimeStampService
) : ITestReportService {
    override fun generateTestReport(reportTitle: String): TestReport {
        val steps = executionTracker.getSteps()

        val htmlTemplate = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$reportTitle Report</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                th, td { border: 1px solid black; padding: 10px; text-align: left; }
                th { background-color: #f2f2f2; }
            </style>
        </head>
        <body>
            <h2>$reportTitle Report</h2>
            <table>
                <tr>
                    <th>Step Name</th>
                    <th>Start Time</th>
                    <th>End Time</th>
                    <th>Duration (ms)</th>
                </tr>
                ${
            steps.joinToString("\n") { step ->
                """
                    <tr>
                        <td>${step.stepName}</td>
                        <td>${step.startTime}</td>
                        <td>${step.endTime}</td>
                        <td>${step.durationMillis} milliseconds</td>
                    </tr>
                    """.trimIndent()
            }
        }
            </table>
        </body>
        </html>
    """.trimIndent()
        return TestReport(htmlTemplate, timeStampService.getCurrentTimestamp())
    }
}
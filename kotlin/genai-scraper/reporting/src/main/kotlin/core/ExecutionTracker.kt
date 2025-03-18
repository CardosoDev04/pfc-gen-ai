package core

import domain.classes.StepExecution
import domain.interfaces.IExecutionTracker
import domain.interfaces.ITimeStampService

class ExecutionTracker(
    private val timeStampService: ITimeStampService
): IExecutionTracker {
    private val stepsTaken = mutableListOf<StepExecution>()

    override fun recordStep(stepName: String, action: () -> Any): Any {
        val startTime = timeStampService.getCurrentTimestamp()
        val result = action()
        val endTime = timeStampService.getCurrentTimestamp()
        stepsTaken.add(StepExecution(stepName, startTime, endTime))
        return result
    }

    override suspend fun recordSuspendStep(stepName: String, action: suspend () -> Any): Any {
        val startTime = timeStampService.getCurrentTimestamp()
        val result = action()
        val endTime = timeStampService.getCurrentTimestamp()
        stepsTaken.add(StepExecution(stepName, startTime, endTime))
        return result
    }

    override fun getSteps(): List<StepExecution> = stepsTaken.toList()
}
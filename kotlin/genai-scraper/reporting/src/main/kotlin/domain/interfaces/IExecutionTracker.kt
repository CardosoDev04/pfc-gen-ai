package domain.interfaces

import domain.classes.StepExecution

interface IExecutionTracker {
    fun recordStep(stepName: String, action: () -> Any): Any
    suspend fun recordSuspendStep(stepName: String, action: suspend () -> Any): Any
    fun getSteps(): List<StepExecution>
}
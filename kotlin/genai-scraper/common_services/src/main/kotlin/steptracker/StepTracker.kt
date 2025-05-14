package steptracker

import java.util.UUID

object StepTracker {
    private val stepMap: MutableMap<String, Int> = HashMap()

    fun initializeRun(): String = UUID.randomUUID().toString().also { stepMap[it] = 0 }

    fun incrementStep(identifier: String) {
        stepMap[identifier] = stepMap.getOrDefault(identifier, 0) + 1
    }

    fun getRunStepCount(identifier: String): Int = stepMap[identifier] ?: 0
}
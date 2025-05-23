package steptracker

import java.time.Instant

object StepTracker {
    private val stepMap: MutableMap<Instant, Int> = HashMap()

    fun initializeRun(): Instant = Instant.now().also { stepMap[it] = 0 }

    fun incrementStep(identifier: Instant) {
        stepMap[identifier] = stepMap.getOrDefault(identifier, 0) + 1
    }

    fun getTwoLastRuns(): Pair<Int, Int> {
        val lastTwo = stepMap.keys.sorted().takeLast(2)
        return Pair(stepMap[lastTwo[0]] ?: 0, stepMap[lastTwo[1]] ?: 0)
    }
}
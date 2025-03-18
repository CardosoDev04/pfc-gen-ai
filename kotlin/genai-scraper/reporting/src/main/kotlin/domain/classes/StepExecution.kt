package domain.classes

import java.time.Duration
import java.time.LocalDateTime

data class StepExecution(val stepName: String, val startTime: LocalDateTime, val endTime: LocalDateTime) {
    val durationMillis: Long get() = Duration.between(startTime, endTime).toMillis()
}
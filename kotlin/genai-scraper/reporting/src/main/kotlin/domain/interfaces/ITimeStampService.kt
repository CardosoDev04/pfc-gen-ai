package domain.interfaces

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

interface ITimeStampService {
    fun getCurrentTimestamp(): LocalDateTime
    fun getDifference(unit: ChronoUnit, startTime: LocalDateTime, endTime: LocalDateTime): Long
}
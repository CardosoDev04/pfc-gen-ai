package utils

import domain.interfaces.ITimeStampService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimeStampService: ITimeStampService {
    override fun getCurrentTimestamp(): LocalDateTime = LocalDateTime.now()

    override fun getDifference(unit: ChronoUnit, startTime: LocalDateTime, endTime: LocalDateTime): Long {
        return when(unit) {
            ChronoUnit.DAYS -> ChronoUnit.DAYS.between(startTime, endTime)
            ChronoUnit.HOURS -> ChronoUnit.HOURS.between(startTime, endTime)
            ChronoUnit.MINUTES -> ChronoUnit.MINUTES.between(startTime, endTime)
            ChronoUnit.MILLIS -> ChronoUnit.MILLIS.between(startTime, endTime)
            else -> throw IllegalArgumentException("Unsupported time unit")
        }
    }
}